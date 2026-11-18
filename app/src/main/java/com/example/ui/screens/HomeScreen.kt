package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Book
import com.example.model.LibraryState
import com.example.model.progressPercent
import com.example.ui.components.BookGrid
import com.example.ui.components.BookRow
import com.example.ui.components.BookScroller
import com.example.ui.components.BrandButton
import com.example.ui.components.CategoryChips
import com.example.ui.components.LanguageChips
import com.example.ui.components.SectionHeader
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.LocalReadingColors
import java.util.Locale

@Composable
fun HomeScreen(
    books: List<Book>,
    libraryState: LibraryState,
    widthSizeClass: WindowWidthSizeClass,
    onBookClick: (Book) -> Unit,
    onOpenExplore: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenLanguages: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onLanguageClick: (String) -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val isWide = widthSizeClass != WindowWidthSizeClass.Compact
    val gridColumns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 5
    }
    val chipColumns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 4
    }

    val continueReading = remember(books, libraryState.history, libraryState.progress) {
        libraryState.history.mapNotNull { h ->
            val b = books.find { it.id == h.bookId }
            val p = libraryState.progress[h.bookId]
            if (b != null && p != null) b to p else null
        }.take(4)
    }

    val popular = remember(books) { books.filter { "popular" in it.tags } }

    val recommended = remember(books, libraryState.languages, libraryState.categories) {
        val preferred = books.filter { b ->
            (libraryState.languages.isEmpty() || b.language in libraryState.languages) &&
                (libraryState.categories.isEmpty() || b.category in libraryState.categories)
        }
        (if (preferred.size >= 4) preferred else books.filter { "recommended" in it.tags }).take(5)
    }

    val newArrivals = remember(books) { books.sortedByDescending { it.added }.take(10) }
    val topRated = remember(books) { books.sortedByDescending { it.rating }.take(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isWide) 24.dp else 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1120.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSearch),
                        shape = CircleShape,
                        color = readingColors.card,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Search books, authors, categories…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = readingColors.card,
                        shadowElevation = 2.dp,
                    ) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(readingColors.brandBrush)
                            .clickable(onClick = onOpenProfile),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = libraryState.profile.name.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(readingColors.heroBrush)
                    .padding(if (isWide) 28.dp else 20.dp),
            ) {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            Text(
                                text = "Find Your Next Story",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Explore thousands of books across languages and categories.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            BrandButton(
                                text = "Explore Books",
                                onClick = onOpenExplore,
                                modifier = Modifier.testTag("hero_explore_button"),
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_books),
                            contentDescription = "Books, an open novel and a warm drink",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(16f / 10f)
                                .clip(RoundedCornerShape(18.dp)),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(
                                text = "Find Your Next Story",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Explore thousands of books across languages and categories.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            BrandButton(
                                text = "Explore Books",
                                onClick = onOpenExplore,
                                modifier = Modifier.testTag("hero_explore_button"),
                            )
                        }
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_books),
                            contentDescription = "Books, an open novel and a warm drink",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 10f)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    }
                }
            }

            if (continueReading.isNotEmpty()) {
                Column {
                    SectionHeader(title = "Continue Reading", onViewAll = onOpenLibrary)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        continueReading.forEach { (book, p) ->
                            BookRow(
                                book = book,
                                percent = progressPercent(p.page, p.pages),
                                onClick = { onBookClick(book) },
                            )
                        }
                    }
                }
            }

            Column {
                SectionHeader(title = "Popular Books", onViewAll = onOpenExplore)
                BookScroller(books = popular, onBookClick = onBookClick)
            }

            Column {
                SectionHeader(title = "Recommended for You", onViewAll = onOpenExplore)
                BookGrid(books = recommended, columns = gridColumns, onBookClick = onBookClick)
            }

            Column {
                SectionHeader(title = "New Arrivals", onViewAll = onOpenExplore)
                BookScroller(books = newArrivals, onBookClick = onBookClick)
            }

            Column {
                SectionHeader(title = "Top Rated", onViewAll = onOpenExplore)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topRated.forEach { b ->
                        BookRow(
                            book = b,
                            onClick = { onBookClick(b) },
                            trailing = {
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(readingColors.gold.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = readingColors.gold,
                                        modifier = Modifier.size(13.dp),
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f", b.rating),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = readingColors.goldFg,
                                    )
                                }
                            },
                        )
                    }
                }
            }

            Column {
                SectionHeader(title = "Browse Categories", onViewAll = onOpenCategories)
                CategoryChips(columns = chipColumns, limit = 8, onCategoryClick = onCategoryClick)
            }

            Column {
                SectionHeader(title = "Browse Languages", onViewAll = onOpenLanguages)
                LanguageChips(columns = chipColumns, limit = 8, onLanguageClick = onLanguageClick)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
