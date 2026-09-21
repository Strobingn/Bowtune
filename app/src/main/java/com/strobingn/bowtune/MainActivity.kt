package com.strobingn.bowtune

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.strobingn.bowtune.ui.navigation.BowTuneBottomBar
import com.strobingn.bowtune.ui.navigation.BowTuneNavHost
import com.strobingn.bowtune.ui.theme.BowTuneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BowTuneApp
        setContent {
            val largeText by app.preferences.largeText.collectAsStateWithLifecycle(false)
            val highContrast by app.preferences.highContrast.collectAsStateWithLifecycle(false)
            BowTuneTheme(
                highContrast = highContrast,
                largeText = largeText
            ) {
                val navController = rememberNavController()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BowTuneBottomBar(navController) }
                ) { innerPadding ->
                    BowTuneNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
