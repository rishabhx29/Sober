package com.sober.system.services.widget

import android.content.Context
import android.content.Intent

/**
 * Asks the home-screen widget to re-read state.
 *
 * The widget lives in `:system:receivers`, which this module cannot depend on
 * (that module already depends on this one). A package-scoped broadcast keeps
 * the dependency pointing one way; it only asks for a refresh, so a spoofed
 * copy cannot change anything.
 */
object WidgetRefresh {

    const val ACTION = "com.sober.app.action.REFRESH_WIDGET"

    fun request(context: Context) {
        context.sendBroadcast(Intent(ACTION).setPackage(context.packageName))
    }
}
