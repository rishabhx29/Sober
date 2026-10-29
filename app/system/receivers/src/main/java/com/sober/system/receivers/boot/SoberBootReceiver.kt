package com.sober.system.receivers.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sober.system.services.core.SoberCoreService

class SoberBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            SoberCoreService.start(context)
        }
    }
}
