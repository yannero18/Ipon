package com.ipon.app.ui.screens.reflection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.local.MoodRating
import com.ipon.app.data.model.icon
import com.ipon.app.data.model.label
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.TabularNumberStyle

@Composable
fun DailyReflectionCard(
    viewModelFactory: IponViewModelFactory,
    modifier: Modifier = Modifier
) {
    val viewModel: ReflectionViewModel = viewModel(factory = viewModelFactory)
    val dayInReview by viewModel.uiState.collectAsState()

    var selectedMood by remember(dayInReview.existingReflection) {
        mutableStateOf(dayInReview.existingReflection?.mood)
    }
    var note by remember(dayInReview.existingReflection) {
        mutableStateOf(dayInReview.existingReflection?.note ?: "")
    }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (dayInReview.existingReflection != null) "Today's check-in" else "How's today going?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KapeBrown
                )
                if (dayInReview.transactionCount > 0) {
                    Text(
                        text = "${dayInReview.transactionCount} ${if (dayInReview.transactionCount == 1) "entry" else "entries"} · ${dayInReview.totalSpent.formatPhp()} out",
                        style = MaterialTheme.typography.bodyMedium.merge(TabularNumberStyle),
                        color = KapeBrownSoft
                    )
                } else {
                    Text(
                        text = "Nothing logged yet today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft
                    )
                }
            }
            dayInReview.existingReflection?.let { reflection ->
                Icon(
                    imageVector = reflection.mood.icon(),
                    contentDescription = reflection.mood.label(),
                    tint = OceanTeal,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MoodRating.entries.forEach { mood ->
                    MoodOption(
                        mood = mood,
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood }
                    )
                }
            }

            if (selectedMood != null) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Add a note (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                )

                Button(
                    onClick = {
                        selectedMood?.let { viewModel.saveReflection(it, note.ifBlank { null }) }
                        expanded = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JeepneyOrange),
                    shape = IponShapes.SquircleSm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text("Save", color = RicePaper)
                }
            }
        }
    }
}

@Composable
private fun MoodOption(
    mood: MoodRating,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(IponShapes.SquircleSm)
            .background(if (selected) OceanTeal.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = mood.icon(),
            contentDescription = null,
            tint = if (selected) OceanTeal else KapeBrownSoft,
            modifier = Modifier.size(28.dp).padding(bottom = 4.dp)
        )
        Text(
            text = mood.label(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) OceanTeal else KapeBrownSoft
        )
    }
}
