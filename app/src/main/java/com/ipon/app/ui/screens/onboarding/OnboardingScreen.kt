package com.ipon.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val body: String
)

private val PAGES = listOf(
    OnboardingPage(
        emoji = "\ud83d\udd12",
        title = "No login. No cloud. Just you.",
        body = "Ipon doesn't ask for an account, a password, or an internet connection. " +
            "Everything you log stays on this device -- this app can't send your " +
            "financial data anywhere, because it never asks for permission to."
    ),
    OnboardingPage(
        emoji = "\ud83e\ude99",
        title = "Built around how money actually moves here",
        body = "Categories, merchant recognition, and the whole feel of the app " +
            "are shaped around everyday Philippine spending -- jeepneys, sari-sari " +
            "stores, sweldo, padala -- not a generic template translated after the fact."
    ),
    OnboardingPage(
        emoji = "\ud83e\udeb7",
        title = "Nothing logs itself without you",
        body = "Recurring bills show up as a card to confirm, never a silent entry. " +
            "Envelopes, goals, and insights are all built from what you've actually " +
            "logged -- and you can export or clear everything, any time, from Settings."
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGES.lastIndex

    Scaffold(containerColor = RicePaper) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onFinished) {
                    Text("Skip", color = KapeBrownSoft)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(page = PAGES[pageIndex])
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                PAGES.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (index == pagerState.currentPage) OceanTeal else KapeBrownSoft.copy(alpha = 0.3f))
                    )
                }
            }

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JeepneyOrange),
                shape = IponShapes.SquircleSm,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(if (isLastPage) "Start logging" else "Next", color = RicePaper)
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = page.emoji, fontSize = 64.sp)
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = KapeBrown,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
        )
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = KapeBrownSoft,
            textAlign = TextAlign.Center
        )
    }
}
