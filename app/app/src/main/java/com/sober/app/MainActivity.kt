package com.sober.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sober.app.ui.SoberApp
import com.sober.system.overlay.ui.SectionBlockOverlayController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sectionOverlayController: SectionBlockOverlayController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoberApp()
        }
    }

    override fun onResume() {
        super.onResume()
        // A section or website cover is meant for another app. The accessibility
        // service ignores events from Sober itself, so without this the cover
        // would sit on top of the screen where the rule is removed.
        sectionOverlayController.hide()
    }
}