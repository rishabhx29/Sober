package com.sober.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.sober.app.shortcuts.ShortcutViewModel
import com.sober.app.ui.SoberApp
import com.sober.system.overlay.ui.SectionBlockOverlayController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sectionOverlayController: SectionBlockOverlayController

    private val shortcutViewModel: ShortcutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A shortcut launched while the process was dead arrives here; one
        // launched from Recents arrives in onNewIntent, because the activity
        // is reused rather than recreated. A configuration change replays the
        // same intent through onCreate, so only a fresh start may act on it —
        // otherwise rotating the phone restarts the preset and jumps tabs.
        if (savedInstanceState == null) {
            shortcutViewModel.handleIntent(intent)
        }
        enableEdgeToEdge()
        setContent {
            SoberApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutViewModel.handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // A section or website cover is meant for another app. The accessibility
        // service ignores events from Sober itself, so without this the cover
        // would sit on top of the screen where the rule is removed.
        sectionOverlayController.hide()
    }
}
