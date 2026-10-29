package com.sober.system.overlay.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
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
import com.sober.core.domain.model.DetectionRule
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
 * The window that covers a blocked section.
 *
 * Separate from [BlockOverlayController] on purpose: an app-level block offers
 * an emergency override and owns the PIN flow, while a section block is a
 * "go back" nudge with no exit of its own. Reusing one controller would make
 * every section rule inherit an override path it must never have.
 *
 * It also carries the accessibility guard's message. Both are "a window we put
 * over another app to say no", and only one may be up at a time, so they share
 * the slot; the caller supplies the copy and a stable key.
 */
@Singleton
class SectionBlockOverlayController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themePreferencesRepository: ThemePreferencesRepository
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private data class Copy(
        val title: String,
        val subtitle: String,
        val footnote: String
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var currentKey: String? = null
    private var currentOnBack: (() -> Unit)? = null

    @Volatile
    private var themeMode = ThemeMode.SYSTEM

    init {
        scope.launch {
            themePreferencesRepository.mode().collect { themeMode = it }
        }
    }

    fun show(rule: DetectionRule, onBack: () -> Unit) = showMessage(
        key = "section:${rule.id}",
        title = rule.overlayTitle,
        subtitle = rule.overlaySubtitle,
        footnote = "${rule.sectionName} stays blocked until you turn the rule off in Sober.",
        onBack = onBack
    )

    fun showMessage(
        key: String,
        title: String,
        subtitle: String,
        footnote: String,
        onBack: () -> Unit
    ) {
        mainHandler.post {
            // Content-changed storms re-report the same screen dozens of times a
            // second; rebuilding the window each time would flicker it.
            if (currentKey == key && overlayView != null) return@post
            if (!canDrawOverlays()) return@post
            currentKey = key
            currentOnBack = onBack
            attach(Copy(title, subtitle, footnote))
        }
    }

    fun hide() {
        mainHandler.post {
            currentKey = null
            currentOnBack = null
            detach()
        }
    }

    val isShowing: Boolean get() = overlayView != null

    private fun canDrawOverlays(): Boolean = try {
        android.provider.Settings.canDrawOverlays(context)
    } catch (e: Exception) {
        false
    }

    private fun attach(copy: Copy) {
        val wm = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).also {
            windowManager = it
        }

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
            setViewTreeLifecycleOwner(this@SectionBlockOverlayController)
            setViewTreeViewModelStoreOwner(this@SectionBlockOverlayController)
            setViewTreeSavedStateRegistryOwner(this@SectionBlockOverlayController)

            setContent {
                SoberTheme(darkTheme = resolveDarkTheme()) {
                    SectionBlockOverlayScreen(
                        title = copy.title,
                        subtitle = copy.subtitle,
                        footnote = copy.footnote,
                        onBack = { currentOnBack?.invoke() }
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Let the platform blur what is underneath where it supports
            // cross-window blur; the scrim alone is the fallback.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wm.isCrossWindowBlurEnabled) {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = (20 * context.resources.displayMetrics.density).toInt()
            }
        }

        try {
            wm.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            // Overlay permission can be revoked between the check and addView.
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

    private fun resolveDarkTheme(): Boolean = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> {
            val uiMode = context.resources.configuration.uiMode
            (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
