package com.example.terminallauncher

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.terminallauncher.ui.TerminalScreen
import com.example.terminallauncher.ui.theme.TerminalLauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Transparent system bars blending edge-to-edge with terminal themes
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            TerminalLauncherTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TerminalScreen()
                }
            }
        }
    }
}
