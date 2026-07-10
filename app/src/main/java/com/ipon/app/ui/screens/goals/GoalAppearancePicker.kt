package com.ipon.app.ui.screens.goals

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ipon.app.ui.components.GoalIconCatalog
import com.ipon.app.ui.components.GoalRingAvatar
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaperDeep
import com.ipon.app.ui.theme.Terracotta

/**
 * Full-width appearance picker for the Create/Edit Goal screens: a live
 * preview ring, a scrollable row of icon+color badges from
 * [GoalIconCatalog], and a system Photo Picker button. Picking an icon
 * clears any chosen photo and vice versa -- exactly one wins, matching how
 * [com.ipon.app.ui.components.GoalRingAvatar] renders (photo first, then
 * catalog icon, then raw emoji as the last-resort fallback for goals
 * created before this catalog existed).
 */
@Composable
fun GoalAppearancePicker(
    currentEmoji: String,
    currentUri: String?,
    onEmojiSelected: (String) -> Unit,
    onUriSelected: (String?) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onUriSelected(uri.toString()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Appearance", style = MaterialTheme.typography.labelMedium, color = KapeBrownSoft)
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            GoalRingAvatar(
                imageUri = currentUri,
                emoji = currentEmoji,
                progress = 0.3f,
                size = 72.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                TextButton(
                    onClick = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = OceanTeal, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (currentUri == null) "Choose photo" else "Change photo", color = OceanTeal)
                }
                if (currentUri != null) {
                    TextButton(onClick = { onUriSelected(null) }) {
                        Text("Remove photo", color = Terracotta, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Or pick an icon", style = MaterialTheme.typography.labelSmall, color = KapeBrownSoft)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GoalIconCatalog.choices.forEach { choice ->
                val isSelected = currentUri == null && currentEmoji == choice.key
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(choice.accentColor.copy(alpha = if (isSelected) 0.32f else 0.14f))
                        .clickable {
                            onEmojiSelected(choice.key)
                            onUriSelected(null)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = choice.icon,
                        contentDescription = null,
                        tint = choice.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
