package com.sober.system.overlay.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sober.core.domain.repository.PinRepository
import com.sober.core.domain.repository.SessionStateRepository
import com.sober.core.domain.repository.ThemeMode
import com.sober.core.domain.repository.ThemePreferencesRepository
import com.sober.core.domain.usecase.BlockDecision
import com.sober.core.domain.usecase.ManageEmergencyOverrideUseCase
import com.sober.core.domain.usecase.OverrideState
import com.sober.core.domain.usecase.VerifyPinUseCase
import com.sober.core.ui.theme.SoberTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the blocking overlay as a true system overlay window.
 *
 * Previously the block screen was a normal `ComponentActivity` started with
 * `startActivity`, which means the system switched away from the app being
 * blocked: the user lands on a Sober screen, and the blocked app drops out of
 * the foreground. `SYSTEM_ALERT_WINDOW` was declared and requested in
 * onboarding but never actually used.
 *
 * This renders a `TYPE_APPLICATION_OVERLAY` window on top of whatever is
 * foreground instead, which is what "no peek-through" requires.
 *
 * Compose inside a WindowManager window has no activity to inherit owners
 * from, so this class supplies the three owners Compose needs and forwards
 * lifecycle events itself.
 *
 * The emergency override lives inside this window rather than in an Activity
 * so it works on top of the blocked app: it is entered on an in-window number
 * pad, which avoids needing a focusable window (and an IME) for what is meant
 * to be a full-screen block.
 */
@Singleton
class BlockOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manageEmergencyOverrideUseCase: ManageEmergencyOverrideUseCase,
    private val verifyPinUseCase: VerifyPinUseCase,
    private val pinRepository: PinRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val themePreferencesRepository: ThemePreferencesRepository
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    /**
     * Owns the override interactions. Main-confined because every caller is a
     * Compose tap handler, and the suspend reads it performs (DataStore) switch
     * dispatchers themselves.
     */
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    /** The decision currently rendered, or null when nothing is showing. */
    @Volatile
    private var currentDecision: BlockDecision? = null

    @Volatile
    private var hasPin = false

    @Volatile
    private var overrideState = OverrideState()

    @Volatile
    private var themeMode = ThemeMode.SYSTEM

    /** True while a window is attached, so the service can re-show after a hide. */
    val isShowing: Boolean get() = overlayView != null

    init {
        // Kept warm for the process lifetime: the overlay can be requested at
        // any moment and must render the current PIN/override state on its
        // first frame, not after an async read.
        scope.launch {
            pinRepository.hasPin.collect {
                hasPin = it
                renderIfShowing()
            }
        }
        scope.launch {
            manageEmergencyOverrideUseCase.state.collect {
                overrideState = it
                renderIfShowing()
            }
        }
        scope.launch {
            themePreferencesRepository.mode().collect {
                themeMode = it
                renderIfShowing()
            }
        }
    }

    fun show(decision: BlockDecision) {
        currentDecision = decision
        mainHandler.post {
            if (!canDrawOverlays()) return@post
            attach(decision)
        }
    }

    fun hide() {
        currentDecision = null
        mainHandler.post { detach() }
    }

    /**
     * Rebuilds the window after the override or PIN state changed. The rebuild
     * is intentional: a request that becomes valid must replace the keypad with
     * the wait state, and Compose-local state must not survive that transition.
     */
    private fun renderIfShowing() {
        mainHandler.post {
            val decision = currentDecision ?: return@post
            if (!canDrawOverlays()) return@post
            attach(decision)
        }
    }

    private fun canDrawOverlays(): Boolean = try {
        android.provider.Settings.canDrawOverlays(context)
    } catch (e: Exception) {
        false
    }

    /**
     * Resolved once per attach, not inside composition: `isSystemInDarkTheme()`
     * is unavailable here because the overlay's ComposeView has no Activity to
     * supply a configuration change host, so SYSTEM is read from resources.
     */
    private fun resolveDarkTheme(): Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            val uiMode = context.resources.configuration.uiMode
            (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * Resolves what to show for a blocked package.
     *
     * The overlay cannot guess a human name from the package id: taking the
     * last segment turns `com.instagram.android` into "Android" and
     * `com.google.android.youtube` into "Youtube" only by luck. So the label
     * comes from PackageManager, with a best-effort fallback that skips generic
     * segments when the lookup fails.
     */
    private fun resolveAppIdentity(packageName: String): Pair<String, Drawable?> {
        if (packageName == context.packageName) return SOBER_NAME to null

        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(info).toString()
            val icon = try {
                pm.getApplicationIcon(info)
            } catch (e: Exception) {
                null
            }
            label to icon
        } catch (e: Exception) {
            fallbackLabel(packageName) to null
        }
    }

    private fun fallbackLabel(packageName: String): String {
        val candidate = packageName
            .split('.')
            .lastOrNull { it.lowercase() !in GENERIC_PACKAGE_SEGMENTS }
            ?: packageName
        return candidate.replaceFirstChar { it.uppercase() }
    }

    private companion object {
        const val SOBER_NAME = "Sober"

        /**
         * Segments that describe the vendor or the platform rather than the
         * app, so they make poor last-resort names.
         */
        val GENERIC_PACKAGE_SEGMENTS = setOf(
            "com", "org", "net", "io", "co", "me",
            "android", "google", "app", "apps", "mobile"
        )
    }

    private fun overrideUiFor(decision: BlockDecision): OverrideUi {
        val current = overrideState
        val requestMatchesApp = current.requestedPackage == decision.packageName
        val requestedAt = if (requestMatchesApp) current.requestedAt else null
        return OverrideUi(
            hasPin = hasPin,
            requestedAt = requestedAt,
            readyAt = requestedAt?.plus(ManageEmergencyOverrideUseCase.WAIT_MILLIS)
        )
    }

    private fun attach(decision: BlockDecision) {
        val wm = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        windowManager = wm

        detach()

        if (lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED) {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        // Compose resolves theme attributes from its host context. The
        // application context has no window theme, so wrap it in the overlay's
        // own theme before constructing the ComposeView.
        val themedContext = ContextThemeWrapper(
            context,
            com.sober.system.overlay.R.style.Theme_Sober
        )

        val (appLabel, appIcon) = resolveAppIdentity(decision.packageName)
        val override = overrideUiFor(decision)

        val view = ComposeView(themedContext).apply {
            // Compose resolves its Recomposer from the view tree, so the three
            // owners must be installed before setContent. AndroidX hides the
            // old ViewTreeXxxOwner classes in newer Lifecycle releases; the
            // extension functions are the supported entry point.
            setViewTreeLifecycleOwner(this@BlockOverlayController)
            setViewTreeViewModelStoreOwner(this@BlockOverlayController)
            setViewTreeSavedStateRegistryOwner(this@BlockOverlayController)

            setContent {
                // The overlay is its own window, so it cannot inherit the
                // app's theme; it reads the same stored preference instead.
                SoberTheme(darkTheme = resolveDarkTheme()) {
                    BlockOverlayScreen(
                        decision = decision,
                        appLabel = appLabel,
                        appIcon = appIcon,
                        override = override,
                        onVerifyPin = { pin, onResult ->
                            scope.launch { onResult(verifyPinUseCase(pin)) }
                        },
                        onRequestOverride = {
                            scope.launch {
                                manageEmergencyOverrideUseCase.request(decision.packageName)
                            }
                        },
                        onCancelRequest = {
                            scope.launch { manageEmergencyOverrideUseCase.cancelRequest() }
                        },
                        onWaitElapsed = { grantOverride(decision.packageName) }
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            // API 26+ (minSdk) guarantees this type exists.
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not focusable: the user cannot type into it, and it must not trap
            // hardware keys. Not touch-modal false either — it must swallow
            // taps to stop the blocked app receiving them underneath. The
            // override panel therefore uses its own on-screen keypad.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Fills the screen; opaque cream background, no blur, no peek-through.
            x = 0
            y = 0
        }

        try {
            wm.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            // addView throws if overlay permission was revoked between the
            // check and the call. Drop the overlay rather than crash the
            // service; the next poll re-checks permission.
            overlayView = null
        }
    }

    /**
     * Called when the wait timer reaches zero. Re-checked against the stored
     * state instead of trusting the UI: the request may have expired or been
     * cancelled while the countdown was running.
     */
    private fun grantOverride(packageName: String) {
        scope.launch {
            val latest = manageEmergencyOverrideUseCase.state.first()
            if (!manageEmergencyOverrideUseCase.canGrant(
                    latest,
                    packageName,
                    System.currentTimeMillis()
                )
            ) {
                // Stale request: drop it so the overlay falls back to idle
                // instead of offering a lift nobody waited for.
                manageEmergencyOverrideUseCase.cancelRequest()
                return@launch
            }
            val sessionId = sessionStateRepository.activeSessionId.first()
            manageEmergencyOverrideUseCase.grant(packageName, sessionId)
            hide()
        }
    }

    private fun detach() {
        val view = overlayView ?: return
        try {
            windowManager?.removeViewImmediate(view)
        } catch (e: Exception) {
            // Already detached by the system.
        }
        overlayView = null
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }
}
