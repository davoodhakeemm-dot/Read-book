package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.Book
import com.example.ui.components.BookGrid
import com.example.ui.components.EmptyState
import com.example.ui.components.PageHeader
import com.example.ui.components.PillChip
import com.example.ui.theme.LocalReadingColors

private val EXPLORE_TABS = listOf("Popular", "Trending", "New", "Recommended", "Top Rated")

@Composable
fun ExploreScreen(
    books: List<Book>,
    widthSizeClass: WindowWidthSizeClass,
    onBookClick: (Book) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenCategories: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    var activeTab by rememberSaveable { mutableStateOf("Popular") }
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 5
    }

    val filtered = remember(books, activeTab) {
        when (activeTab) {
            "Popular" -> books.filter { "popular" in it.tags }
            "Trending" -> books.filter { "trending" in it.tags }
            "New" -> books.sortedByDescending { it.added }.take(12)
            "Recommended" -> books.filter { "recommended" in it.tags }
            else -> books.sortedByDescending { it.rating }.take(12)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.Compact) 16.dp else 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1120.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PageHeader(
                title = "Explore Books",
                subtitle = "Discover something new in every language.",
                action = if (widthSizeClass != WindowWidthSizeClass.Compact) {
                    {
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(onClick = onOpenSearch),
                            shape = CircleShape,
                            color = readingColors.card,
                            shadowElevation = 2.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Search",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else null,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EXPLORE_TABS.forEach { tab ->
                    PillChip(
                        label = tab,
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        modifier = Modifier.testTag("explore_tab_${tab.lowercase().replace(" ", "_")}"),
                    )
                }
            }

            if (filtered.isNotEmpty()) {
                BookGrid(books = filtered, columns = columns, onBookClick = onBookClick)
            } else {
                EmptyState(
                    title = "No Books Found",
                    description = "Try another search or category.",
                    actionLabel = "Explore Books",
                    onAction = onOpenCategories,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
