package com.sober.system.overlay.ui

import android.content.Context
import android.content.Intent
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

    @Volatile
    private var themeMode = ThemeMode.SYSTEM

    /** Attachment, not just the field: the system can detach the window itself. */
    val isShowing: Boolean
        get() = overlayView?.let { it.isAttachedToWindow && it.isShown } == true

    init {
        scope.launch {
            themePreferencesRepository.mode().collect { themeMode = it }
        }
    }

    /**
     * Shows the countdown for [packageName]. Calling it again for the same app
     * while the window is up is a no-op: rebuilding would restart the clock and
     * a fifteen-second poll would then never let the user through.
     */
    fun show(packageName: String, delaySeconds: Int) {
        if (currentPackage == packageName && isShowing) return
        currentPackage = packageName
        mainHandler.post {
            if (!canDrawOverlays()) return@post
            attach(packageName, delaySeconds)
        }
    }

    fun hide() {
        currentPackage = null
        mainHandler.post { detach() }
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
                        passMinutes = PASS_MINUTES,
                        onOpen = { grantPassAndHide(packageName) },
                        onClose = {
                            hide()
                            goHome()
                        }
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

    private fun grantPassAndHide(packageName: String) {
        scope.launch {
            pauseRulesRepository.grantPass(
                packageName = packageName,
                until = System.currentTimeMillis() + PASS_MINUTES * 60_000L
            )
            hide()
        }
    }

    private fun goHome() {
        try {
            context.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // No launcher resolved; the app stays open, which is not harmful.
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
