package com.sober.system.overlay.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
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
import com.sober.core.domain.repository.PauseRulesRepository
import com.sober.core.domain.repository.ThemeMode
import com.sober.core.domain.repository.ThemePreferencesRepository
import com.sober.core.ui.theme.SoberTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the pause-before-opening overlay.
 *
 * A separate window from [BlockOverlayController] because the two mean opposite
 * things: the block screen refuses the app, and this one delays it while still
 * letting the user choose. Keeping them apart also means a pause can never
 * replace a block that is in force, or the other way round.
 *
 * Like the block overlay, Compose needs the three owners supplied manually, and
 * the window is not focusable so hardware back cannot skip the countdown.
 */
@Singleton
class PauseOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pauseRulesRepository: PauseRulesRepository,
    private val themePreferencesRepository: ThemePreferencesRepository
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    /** The package this window is currently counting down for, or null. */
    @Volatile
    private var currentPackage: String? = null

    /** Guards against the Compose clock and the handler both completing. */
    @Volatile
    private var completedPackage: String? = null

    /** Told when the countdown ends, so the caller can decide what is next. */
    @Volatile
    private var onElapsed: (() -> Unit)? = null

    @Volatile
    private var themeMode = ThemeMode.SYSTEM

    /** Attachment, not just the field: the system can detach the window itself. */
    val isShowing: Boolean
        get() = overlayView?.let { it.isAttachedToWindow && it.isShown } == true

    /** True while this window is counting down for [packageName]. */
    fun isShowingFor(packageName: String): Boolean =
        currentPackage == packageName && isShowing

    init {
        scope.launch {
            themePreferencesRepository.mode().collect { themeMode = it }
        }
    }

    /**
     * Shows the countdown for [packageName]. Calling it again for the same app
     * while the window is up is a no-op: rebuilding would restart the clock and
     * a fifteen-second poll would then never let the user through.
     *
     * [onElapsed] runs once, after the pass has been granted and the window is
     * gone, so it can safely evaluate the next rule (the session picker) for
     * the same app.
     */
    fun show(packageName: String, delaySeconds: Int, onElapsed: () -> Unit) {
        if (currentPackage == packageName && isShowing) return
        currentPackage = packageName
        completedPackage = null
        this.onElapsed = onElapsed
        mainHandler.post {
            if (!canDrawOverlays()) return@post
            attach(packageName, delaySeconds)
        }
    }

    fun hide() {
        currentPackage = null
        completedPackage = null
        onElapsed = null
        mainHandler.post { detach() }
    }

    /**
     * The countdown reached zero. The pass is what stops the next poll from
     * showing the pause again, so it is written before the window goes away.
     */
    private fun completePause(packageName: String) {
        if (currentPackage != packageName) return
        if (completedPackage == packageName) return
        completedPackage = packageName
        val callback = onElapsed ?: return
        scope.launch {
            runCatching {
                pauseRulesRepository.grantPass(
                    packageName = packageName,
                    until = System.currentTimeMillis() + PASS_MINUTES * 60_000L
                )
            }
            hide()
            // hide() posts the detach; this post runs after it, so the callback
            // never sees this window as still showing.
            mainHandler.post(callback)
        }
    }

    private fun canDrawOverlays(): Boolean = try {
        android.provider.Settings.canDrawOverlays(context)
    } catch (e: Exception) {
        false
    }

    private fun resolveDarkTheme(): Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            val uiMode = context.resources.configuration.uiMode
            (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    private fun appLabel(packageName: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName.split('.')
            .lastOrNull { it.lowercase() !in GENERIC_SEGMENTS }
            ?.replaceFirstChar { it.uppercase() }
            ?: packageName
    }

    private fun attach(packageName: String, delaySeconds: Int) {
        val wm = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        windowManager = wm
        detach()

        if (lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED) {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val themedContext = ContextThemeWrapper(
            context,
            com.sober.system.overlay.R.style.Theme_Sober
        )

        val view = ComposeView(themedContext).apply {
            setViewTreeLifecycleOwner(this@PauseOverlayController)
            setViewTreeViewModelStoreOwner(this@PauseOverlayController)
            setViewTreeSavedStateRegistryOwner(this@PauseOverlayController)

            setContent {
                SoberTheme(darkTheme = resolveDarkTheme()) {
                    PauseOverlayScreen(
                        appLabel = appLabel(packageName),
                        delaySeconds = delaySeconds,
                        onElapsed = { completePause(packageName) }
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        try {
            wm.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            // Overlay permission revoked between the check and addView.
            overlayView = null
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

    companion object {
        /** How long one earned pass lasts before the pause applies again. */
        const val PASS_MINUTES = 2

        private val GENERIC_SEGMENTS = setOf(
            "com", "org", "net", "io", "co", "me",
            "android", "google", "app", "apps", "mobile"
        )
    }
}
