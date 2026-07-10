package com.ipon.app.ui.screens.goals

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.util.Money

/**
 * Full-screen "New Goal" flow -- the GoTyme-style dedicated page, replacing
 * the earlier AlertDialog version. Same field set and validation as before;
 * only the container changed, from a boxed modal to a real screen with its
 * own back stack entry, matching how AddTransactionScreen and
 * AddRecurringTemplateScreen already work in this app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(
    viewModelFactory: IponViewModelFactory,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: GoalsViewModel = viewModel(factory = viewModelFactory)

    var label by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf(com.ipon.app.ui.components.GoalIconCatalog.choices.first().key) }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var labelError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text("New Goal", color = KapeBrown, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Cancel", color = KapeBrownSoft) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RicePaper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            GoalAppearancePicker(
                currentEmoji = selectedEmoji,
                currentUri = selectedImageUri,
                onEmojiSelected = { selectedEmoji = it },
                onUriSelected = { selectedImageUri = it }
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it; labelError = null },
                label = { Text("What are you saving for?") },
                isError = labelError != null,
                supportingText = labelError?.let { errorText -> { Text(errorText) } },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            )
            OutlinedTextField(
                value = target,
                onValueChange = { target = it; amountError = null },
                label = { Text("Target Amount (Php)") },
                isError = amountError != null,
                supportingText = amountError?.let { errorText -> { Text(errorText) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
            OutlinedTextField(
                value = deadline,
                onValueChange = { deadline = it },
                label = { Text("Deadline (e.g. Dec 2026) - Optional") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )

            Button(
                onClick = {
                    val amount = target.toLongOrNull() ?: 0L
                    val isLabelValid = label.isNotBlank()
                    val isAmountValid = amount > 0L
                    labelError = if (!isLabelValid) "Give it a name" else null
                    amountError = if (!isAmountValid) "Enter a valid target amount" else null
                    if (isLabelValid && isAmountValid) {
                        viewModel.createGoal(
                            label,
                            Money.ofMinorUnits(amount * 100),
                            selectedEmoji,
                            deadline.takeIf { it.isNotBlank() },
                            selectedImageUri
                        )
                        onSaved()
                    }
                },
                shape = IponShapes.SquircleLg,
                colors = ButtonDefaults.buttonColors(containerColor = OceanTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Text("Create Goal", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
