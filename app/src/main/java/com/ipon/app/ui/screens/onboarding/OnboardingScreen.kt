package com.ipon.app.ui.screens.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipon.app.ui.components.KapePalette
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String
)

private val PAGES = listOf(
    OnboardingPage(
        icon = Icons.Outlined.Lock,
        title = "No login. No cloud. Just you.",
        body = "Ipon doesn't ask for an account, a password, or an internet connection. " +
            "Everything you log stays on this device -- this app can't send your " +
            "financial data anywhere, because it never asks for permission to."
    ),
    OnboardingPage(
        icon = Icons.Outlined.Savings,
        title = "Built around how money actually moves here",
        body = "Categories, merchant recognition, and the whole feel of the app " +
            "are shaped around everyday Philippine spending -- jeepneys, sari-sari " +
            "stores, sweldo, padala -- not a generic template translated after the fact."
    ),
    OnboardingPage(
        icon = Icons.Outlined.TaskAlt,
        title = "Nothing logs itself without you",
        body = "Recurring bills show up as a card to confirm, never a silent entry. " +
            "Envelopes, goals, and insights are all built from what you've actually " +
            "logged -- and you can export or clear everything, any time, from Settings."
    )
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: (name: String, paydays: List<Int>) -> Unit) {
    // 0: Slides, 1: Name Input, 2: Payday Input
    var onboardingStep by remember { mutableStateOf(0) } 
    var nameInput by remember { mutableStateOf("") }
    var paydaysInput by remember { mutableStateOf("") }
    
    val isNameValid = nameInput.trim().isNotBlank()

    val buttonAsymmetricShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomEnd = 20.dp,
        bottomStart = 12.dp
    )

    if (onboardingStep == 0) {
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
                    TextButton(onClick = { onboardingStep = 1 }) {
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
                            onboardingStep = 1
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                    shape = IponShapes.SquircleSm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text(if (isLastPage) "Next" else "Next", color = RicePaper)
                }
            }
        }
    } else if (onboardingStep == 1) {
        Scaffold(containerColor = KapePalette.BackgroundWarm) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Spacer / Brand Block
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🪙",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "Welcome to Ipon",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = KapePalette.TextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "An authentic, editorial ledger built for traditional Philippine financial habits.",
                        fontSize = 14.sp,
                        color = KapePalette.TextPrimary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                // Input Core Form Block
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "What should we call you?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KapePalette.TextPrimary
                    )
                    
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("e.g., Yannero", color = KapePalette.TextPrimary.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KapePalette.HeroCardSolid,
                            unfocusedBorderColor = KapePalette.TextPrimary.copy(alpha = 0.2f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Action Confirmation Button
                Button(
                    onClick = { if (isNameValid) onboardingStep = 2 },
                    enabled = isNameValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KapePalette.HeroCardSolid,
                        disabledContainerColor = KapePalette.HeroCardSolid.copy(alpha = 0.3f)
                    ),
                    shape = buttonAsymmetricShape
                ) {
                    Text(
                        text = "Next ➔",
                        color = KapePalette.TextLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        // Step 2: Payday Configuration
        Scaffold(containerColor = KapePalette.BackgroundWarm) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📅",
                        fontSize = 64.sp
                    )
                    Text(
                        text = "When do you get paid?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = KapePalette.TextPrimary,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ipon calculates exactly how much you can safely spend per day to make your balance last until your next sweldo.",
                        fontSize = 14.sp,
                        color = KapePalette.TextPrimary.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter days separated by commas:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KapePalette.TextPrimary
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = paydaysInput == "15, 30",
                            onClick = { paydaysInput = "15, 30" },
                            label = { Text("15th & 30th", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OceanTeal,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = paydaysInput == "1",
                            onClick = { paydaysInput = "1" },
                            label = { Text("Every 1st", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OceanTeal,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    OutlinedTextField(
                        value = paydaysInput,
                        onValueChange = { paydaysInput = it },
                        placeholder = { Text("e.g. 15, 30", color = KapePalette.TextPrimary.copy(alpha = 0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KapePalette.HeroCardSolid,
                            unfocusedBorderColor = KapePalette.TextPrimary.copy(alpha = 0.2f),
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Button(
                    onClick = { 
                        val parsedDays = paydaysInput.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..31 }
                        val finalDays = if (parsedDays.isNotEmpty()) parsedDays else listOf(15, 30)
                        onFinished(nameInput.trim(), finalDays) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KapePalette.HeroCardSolid),
                    shape = buttonAsymmetricShape
                ) {
                    Text(
                        text = "Finish & Get Started",
                        color = KapePalette.TextLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(IponShapes.SquircleLg)
                .background(OceanTeal.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = OceanTeal,
                modifier = Modifier.size(44.dp)
            )
        }
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