package com.sober.system.receivers.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The home-screen widget's update entry point.
 *
 * State reads are suspending, so each update uses `goAsync` to keep the
 * process alive until the widget is written. The period in the widget info is
 * a floor of thirty minutes; real updates come from [SoberWidgetActionReceiver]
 * refreshes the engine sends when protection state changes.
 */
@AndroidEntryPoint
class SoberStatusWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var renderer: SoberWidgetRenderer

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderer.render(context)
            } catch (e: Exception) {
                // A widget that cannot read its state must not take the
                // process down; the next update or refresh tries again.
            } finally {
                pending.finish()
            }
        }
    }
}
