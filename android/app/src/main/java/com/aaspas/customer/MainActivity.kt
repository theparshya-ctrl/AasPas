package com.aaspas.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aaspas.customer.core.startup.StartupTracer
import com.aaspas.customer.presentation.navigation.AasPasNavHost
import com.aaspas.customer.presentation.theme.AasPasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StartupTracer.mark("main_activity_onCreate")
        enableEdgeToEdge()
        setContent {
            StartupTracer.mark("main_activity_setContent")
            AasPasTheme {
                AasPasNavHost()
            }
        }
    }
}
