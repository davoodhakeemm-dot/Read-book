package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookRepository
import com.example.model.Book
import com.example.model.CATEGORIES
import com.example.model.LANGUAGES
import com.example.model.SearchSort
import com.example.ui.components.BookGrid
import com.example.ui.components.EmptyState
import com.example.ui.components.PageHeader
import com.example.ui.components.PillChip
import com.example.ui.theme.LocalReadingColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    books: List<Book>,
    repository: BookRepository,
    widthSizeClass: WindowWidthSizeClass,
    onBookClick: (Book) -> Unit,
    onExploreClick: () -> Unit,
    onSecretChatTrigger: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedLanguages by remember { mutableStateOf(emptySet<String>()) }
    var selectedCategories by remember { mutableStateOf(emptySet<String>()) }
    var minRating by rememberSaveable { mutableDoubleStateOf(0.0) }
    var sort by rememberSaveable { mutableStateOf(SearchSort.POPULAR) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 5
    }

    val results = remember(books, query, selectedLanguages, selectedCategories, minRating, sort) {
        repository.searchBooks(
            books = books,
            query = query,
            languages = selectedLanguages,
            categories = selectedCategories,
            minRating = minRating,
            sort = sort,
        )
    }

    val activeFilterCount = selectedLanguages.size + selectedCategories.size + if (minRating > 0.0) 1 else 0

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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PageHeader(
                title = "Search",
                subtitle = "Books, authors, categories and languages.",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    color = readingColors.card,
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search books, authors or categories…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { value ->
                                    if (value.trim().equals("hi", ignoreCase = true)) {
                                        query = ""
                                        onSecretChatTrigger()
                                    } else {
                                        query = value
                                    }
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input"),
                            )
                        }
                        if (query.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .clickable { query = "" },
                            )
                        }
                    }
                }

                Box {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { showFilters = !showFilters }
                            .testTag("search_filter_toggle"),
                        shape = CircleShape,
                        color = if (showFilters) MaterialTheme.colorScheme.primary else readingColors.card,
                        shadowElevation = 2.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Filters",
                                tint = if (showFilters) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (activeFilterCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = activeFilterCount.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onError,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showFilters) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = readingColors.card,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FilterGroup(title = "Language") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                LANGUAGES.forEach { lang ->
                                    val selected = lang.code in selectedLanguages
                                    PillChip(
                                        label = "${lang.flag} ${lang.name}",
                                        selected = selected,
                                        onClick = {
                                            selectedLanguages = if (selected) {
                                                selectedLanguages - lang.code
                                            } else {
                                                selectedLanguages + lang.code
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        FilterGroup(title = "Category") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CATEGORIES.forEach { cat ->
                                    val selected = cat.slug in selectedCategories
                                    PillChip(
                                        label = "${cat.icon} ${cat.name}",
                                        selected = selected,
                                        onClick = {
                                            selectedCategories = if (selected) {
                                                selectedCategories - cat.slug
                                            } else {
                                                selectedCategories + cat.slug
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        FilterGroup(title = "Minimum rating") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(0.0, 4.0, 4.5, 4.7).forEach { r ->
                                    PillChip(
                                        label = if (r == 0.0) "Any" else "$r+ ★",
                                        selected = minRating == r,
                                        onClick = { minRating = r },
                                    )
                                }
                            }
                        }

                        if (activeFilterCount > 0) {
                            Text(
                                text = "Clear all filters",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedLanguages = emptySet()
                                        selectedCategories = emptySet()
                                        minRating = 0.0
                                    }
                                    .padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${results.size} ${if (results.size == 1) "book" else "books"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchSort.entries.forEach { s ->
                        PillChip(
                            label = s.label,
                            selected = sort == s,
                            onClick = { sort = s },
                        )
                    }
                }
            }

            if (results.isNotEmpty()) {
                BookGrid(books = results, columns = columns, onBookClick = onBookClick)
            } else {
                EmptyState(
                    title = "No Books Found",
                    description = "Try another search or category.",
                    actionLabel = "Explore Books",
                    onAction = onExploreClick,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilterGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
