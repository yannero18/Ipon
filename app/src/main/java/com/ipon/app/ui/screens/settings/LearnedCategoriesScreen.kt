package com.ipon.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipon.app.data.local.TransactionType
import com.ipon.app.data.repository.LearnedCategoryMemory
import com.ipon.app.di.IponViewModelFactory
import com.ipon.app.ui.components.CategoryLabel
import com.ipon.app.ui.theme.IponShapes
import com.ipon.app.ui.theme.KapeBrown
import com.ipon.app.ui.theme.KapeBrownSoft
import com.ipon.app.ui.theme.RicePaper
import com.ipon.app.ui.theme.Terracotta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnedCategoriesScreen(
    viewModelFactory: IponViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: LearnedCategoriesViewModel = viewModel(factory = viewModelFactory)
    val memories by viewModel.memories.collectAsState()

    Scaffold(
        containerColor = RicePaper,
        topBar = {
            TopAppBar(
                title = { Text("Your learned categories", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = KapeBrownSoft) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Whenever you correct or confirm a category for a merchant, Ipon " +
                        "remembers it here and uses it ahead of its generic suggestions next " +
                        "time. Nothing here is shared anywhere -- it's just what you've taught " +
                        "the app about your own spending.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KapeBrownSoft,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }

            if (memories.isEmpty()) {
                item {
                    Text(
                        text = "Nothing learned yet. Log a few transactions with merchant names " +
                            "and they'll start showing up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KapeBrownSoft
                    )
                }
            }

            items(memories, key = { it.merchantKey + it.type.name }) { memory ->
                MemoryRow(
                    memory = memory,
                    onForget = { viewModel.forget(memory) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MemoryRow(
    memory: LearnedCategoryMemory,
    onForget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(IponShapes.SquircleLg)
            .background(Color.White)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = memory.merchantKey,
                style = MaterialTheme.typography.bodyLarge,
                color = KapeBrown
            )
            CategoryLabel(
                category = memory.category,
                text = "${memory.category.displayName} \u00b7 ${typeLabel(memory.type)} \u00b7 ${memory.confirmCount}\u00d7",
                iconSize = 14.dp,
                tint = KapeBrownSoft,
                textColor = KapeBrownSoft,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        TextButton(onClick = onForget) {
            Text("Forget", color = Terracotta)
        }
    }
}

private fun typeLabel(type: TransactionType): String = when (type) {
    TransactionType.EXPENSE -> "expense"
    TransactionType.INCOME -> "income"
}
