package com.sober.system.receivers.widget

/**
 * Actions the widget's own button sends. They are explicit-component
 * broadcasts to [SoberWidgetActionReceiver], so no other app can send them.
 */
object WidgetActions {
    const val PAUSE = "com.sober.app.action.WIDGET_PAUSE"
    const val RESUME = "com.sober.app.action.WIDGET_RESUME"
}
