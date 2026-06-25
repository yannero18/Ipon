package com.ipon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.navigation.IponNavHost
import com.ipon.app.ui.theme.IponTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as IponApplication).container
        val viewModelFactory = IponViewModelFactory(container)

        setContent {
            IponTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    IponNavHost(viewModelFactory = viewModelFactory)
                }
            }
        }
    }
}
