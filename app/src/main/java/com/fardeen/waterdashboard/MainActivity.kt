package com.fardeen.waterdashboard

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.fardeen.waterdashboard.ui.AquaApp
import com.fardeen.waterdashboard.viewmodel.AquaViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AquaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { AquaApp(viewModel) }
    }
}
