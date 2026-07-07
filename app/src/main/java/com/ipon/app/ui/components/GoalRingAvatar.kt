package com.ipon.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ipon.app.ui.theme.JeepneyOrange
import com.ipon.app.ui.theme.OceanTeal
import com.ipon.app.ui.theme.RicePaper

/**
 * The core visual component of the new Goals redesign (GoTyme/GoSave style).
 * Displays a circular photo (or emoji fallback) wrapped in a progress ring.
 * Full ring = goal complete (Teal); Partial ring = in progress (Orange).
 */
@Composable
fun GoalRingAvatar(
    imageUri: String?,
    emoji: String,
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 4.dp
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val isComplete = clampedProgress >= 1f
    
    // Teal when complete, Orange when building momentum
    val ringColor = if (isComplete) OceanTeal else JeepneyOrange

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Faded background track
        CircularProgressIndicator(
            progress = 1f,
            modifier = Modifier.fillMaxSize(),
            color = ringColor.copy(alpha = 0.15f),
            strokeWidth = strokeWidth
        )
        
        // Active progress track
        CircularProgressIndicator(
            progress = clampedProgress,
            modifier = Modifier.fillMaxSize(),
            color = ringColor,
            strokeWidth = strokeWidth
        )
        
        // Inner Avatar (Photo or Emoji)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(strokeWidth + 2.dp)
                .clip(CircleShape)
                .background(RicePaper),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(imageUri),
                    contentDescription = "Goal Photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = emoji,
                    fontSize = (size.value * 0.4f).sp
                )
            }
        }
    }
}