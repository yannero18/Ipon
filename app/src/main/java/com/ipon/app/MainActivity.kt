package com.ipon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.navigation.IponNavHost
import com.ipon.app.ui.screens.onboarding.OnboardingScreen
import com.ipon.app.ui.theme.IponTheme
import com.ipon.app.util.OnboardingPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as IponApplication).container
        val viewModelFactory = IponViewModelFactory(container)
        val onboardingPreferences = OnboardingPreferences(applicationContext)

        setContent {
            IponTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Read once per process start, not on every
                    // recomposition -- onboarding's completed state is only
                    // ever written from the screen below itself, never from
                    // elsewhere, so a single initial read plus local state
                    // is enough; no need for this to be a Flow.
                    var hasSeenOnboarding by remember { mutableStateOf(onboardingPreferences.hasSeenOnboarding()) }

                    if (hasSeenOnboarding) {
                        IponNavHost(viewModelFactory = viewModelFactory)
                    } else {
                        OnboardingScreen(
                            onFinished = {
                                onboardingPreferences.markOnboardingSeen()
                                hasSeenOnboarding = true
                            }
                        )
                    }
                }
            }
        }
    }
}
