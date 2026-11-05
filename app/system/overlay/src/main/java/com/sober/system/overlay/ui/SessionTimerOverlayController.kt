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
import com.sober.core.domain.model.AppLimit
import com.sober.core.domain.repository.AppLimitRepository
import com.sober.core.domain.repository.ThemeMode
import com.sober.core.domain.repository.ThemePreferencesRepository
import com.sober.core.domain.usecase.StartAppSessionUseCase
import com.sober.core.ui.theme.SoberTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the session-timer length picker.
 *
 * Its own window, like the pause screen, because it means something different
 * from both other overlays: a block refuses the app and a pause delays it,
 * while this asks the user to commit to a length. Keeping it separate means
 * neither can replace the other, and the enforcement engine can decide which
 * one, if any, is truthful for the current state.
 *
 * Compose needs the three owners supplied manually, and the window is not
 * focusable so hardware back cannot skip the question.
 */
@Singleton
class SessionTimerOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val startAppSessionUseCase: StartAppSessionUseCase,
    private val appLimitRepository: AppLimitRepository,
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

    /** The package this window is asking about, or null. */
    @Volatile
    private var currentPackage: String? = null

    /**
     * True between the tap and the session being written. Without it a poll
     * that runs in that gap would see no record yet and re-attach the picker
     * the user just dismissed.
     */
    @Volatile
    private var startingPackage: String? = null

    @Volatile
    private var themeMode = ThemeMode.SYSTEM

    val isShowing: Boolean
        get() = overlayView?.let { it.isAttachedToWindow && it.isShown } == true

    init {
        scope.launch {
            themePreferencesRepository.mode().collect { themeMode = it }
        }
    }

    /**
     * Shows the picker for [packageName]. Calling it again for the same app is
     * a no-op: rebuilding would reset an already-made selection.
     *
     * The app's configured block time is read first so the picker can state the
     * real rest, instead of asserting a number the session will not honor.
     */
    fun show(packageName: String) {
        if (startingPackage == packageName) return
        if (currentPackage == packageName && isShowing) return
        currentPackage = packageName
        scope.launch {
            val restMinutes = runCatching {
                appLimitRepository.getLimit(packageName)?.sessionRestMinutes
            }.getOrNull()
                ?.coerceIn(AppLimit.MIN_SESSION_REST_MINUTES, AppLimit.MAX_SESSION_REST_MINUTES)
                ?: AppLimit.DEFAULT_SESSION_REST_MINUTES
            mainHandler.post {
                // The user may have left while the rule was being read.
                if (currentPackage != packageName) return@post
                if (!canDrawOverlays()) return@post
                attach(packageName, restMinutes)
            }
        }
    }

    fun hide() {
        currentPackage = null
        mainHandler.post { detach() }
    }

    private fun start(packageName: String, minutes: Int) {
        startingPackage = packageName
        scope.launch {
            try {
                startAppSessionUseCase(packageName, minutes)
            } finally {
                startingPackage = null
                hide()
            }
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

    private fun attach(packageName: String, restMinutes: Int) {
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
            setViewTreeLifecycleOwner(this@SessionTimerOverlayController)
            setViewTreeViewModelStoreOwner(this@SessionTimerOverlayController)
            setViewTreeSavedStateRegistryOwner(this@SessionTimerOverlayController)

            setContent {
                SoberTheme(darkTheme = resolveDarkTheme()) {
                    SessionTimerOverlayScreen(
                        appLabel = appLabel(packageName),
                        durations = DURATIONS,
                        defaultMinutes = DEFAULT_MINUTES,
                        restMinutes = restMinutes,
                        onStart = { minutes -> start(packageName, minutes) },
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
        /**
         * Two minutes is the shortest session that is still a session, and an
         * hour is the longest a single deliberate visit reasonably lasts.
         */
        private val DURATIONS = listOf(2, 5, 10, 15, 30, 60)

        /** The length the picker starts on; the middle of the range. */
        private const val DEFAULT_MINUTES = 10

        private val GENERIC_SEGMENTS = setOf(
            "com", "org", "net", "io", "co", "me",
            "android", "google", "app", "apps", "mobile"
        )
    }
}
