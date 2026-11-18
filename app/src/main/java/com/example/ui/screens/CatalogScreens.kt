package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.CATEGORIES
import com.example.model.LANGUAGES
import com.example.model.categoryBySlug
import com.example.model.languageByCode
import com.example.ui.components.BookCover
import com.example.ui.components.BookGrid
import com.example.ui.components.BrandButton
import com.example.ui.components.CategoryChips
import com.example.ui.components.CoverSize
import com.example.ui.components.EmptyState
import com.example.ui.components.LanguageChips
import com.example.ui.components.PageHeader
import com.example.ui.components.PillChip
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalReadingColors

@Composable
fun CategoriesScreen(
    widthSizeClass: WindowWidthSizeClass,
    onCategoryClick: (String) -> Unit,
) {
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 4
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
            PageHeader(title = "Categories", subtitle = "Sixteen shelves, one library.")
            CategoryChips(columns = columns, onCategoryClick = onCategoryClick)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDetailScreen(
    slug: String,
    books: List<Book>,
    widthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit,
    onReadNow: (Book) -> Unit,
    onCategoryClick: (String) -> Unit,
    onExploreClick: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val category = categoryBySlug(slug)
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 5
    }

    if (category == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Category not found",
                description = "That shelf doesn't exist. Try browsing all categories.",
                actionLabel = "Browse categories",
                onAction = onBack,
            )
        }
        return
    }

    var selectedLang by rememberSaveable(slug) { mutableStateOf("all") }
    var sort by rememberSaveable(slug) { mutableStateOf("popular") }

    val all = remember(books, slug) { books.filter { it.category == slug } }
    val filtered = remember(all, selectedLang, sort) {
        val langFiltered = if (selectedLang == "all") all else all.filter { it.language == selectedLang }
        when (sort) {
            "rating" -> langFiltered.sortedByDescending { it.rating }
            "newest" -> langFiltered.sortedByDescending { it.added }
            else -> langFiltered.sortedByDescending { it.reviews }
        }
    }

    val featured = remember(all) { all.find { "featured" in it.tags } ?: all.firstOrNull() }
    val popular = remember(all) { all.filter { "popular" in it.tags } }
    val fresh = remember(all) { all.sortedByDescending { it.added }.take(5) }
    val recommended = remember(all) { all.filter { "recommended" in it.tags } }

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
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to categories",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "${category.icon} ${category.name} Books",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (featured != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(readingColors.heroBrush)
                        .padding(20.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(104.dp)
                                .clickable { onBookClick(featured) },
                        ) {
                            BookCover(book = featured, size = CoverSize.MD)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FEATURED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = featured.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = featured.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = featured.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            BrandButton(
                                text = "Read Now",
                                onClick = { onReadNow(featured) },
                            )
                        }
                    }
                }
            }

            if (popular.isNotEmpty()) {
                Column {
                    SectionHeader(title = "Popular ${category.name}")
                    BookGrid(books = popular, columns = columns, onBookClick = onBookClick)
                }
            }

            if (fresh.isNotEmpty()) {
                Column {
                    SectionHeader(title = "New in ${category.name}")
                    BookGrid(books = fresh, columns = columns, onBookClick = onBookClick)
                }
            }

            if (recommended.isNotEmpty()) {
                Column {
                    SectionHeader(title = "Recommended")
                    BookGrid(books = recommended, columns = columns, onBookClick = onBookClick)
                }
            }

            Column {
                SectionHeader(title = "All ${category.name} Books")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PillChip(
                        label = "All languages",
                        selected = selectedLang == "all",
                        onClick = { selectedLang = "all" },
                    )
                    LANGUAGES.filter { l -> all.any { it.language == l.code } }.forEach { l ->
                        PillChip(
                            label = "${l.flag} ${l.name}",
                            selected = selectedLang == l.code,
                            onClick = { selectedLang = l.code },
                        )
                    }
                    listOf("popular" to "Popular", "rating" to "Top Rated", "newest" to "Newest").forEach { (key, label) ->
                        PillChip(
                            label = label,
                            selected = sort == key,
                            onClick = { sort = key },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (filtered.isNotEmpty()) {
                    BookGrid(books = filtered, columns = columns, onBookClick = onBookClick)
                } else {
                    EmptyState(
                        title = "No Books Found",
                        description = "Try another language or category.",
                        actionLabel = "Explore Books",
                        onAction = onExploreClick,
                    )
                }
            }

            Column {
                SectionHeader(title = "Other categories")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CATEGORIES.filter { it.slug != slug }.forEach { c ->
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onCategoryClick(c.slug) },
                            shape = CircleShape,
                            color = readingColors.card,
                            shadowElevation = 2.dp,
                        ) {
                            Text(
                                text = "${c.icon} ${c.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguagesScreen(
    widthSizeClass: WindowWidthSizeClass,
    onLanguageClick: (String) -> Unit,
) {
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 4
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
            PageHeader(title = "Languages", subtitle = "Stories in the language you think in.")
            LanguageChips(columns = columns, onLanguageClick = onLanguageClick)
        }
    }
}

@Composable
fun LanguageDetailScreen(
    code: String,
    books: List<Book>,
    widthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onBookClick: (Book) -> Unit,
    onLanguageClick: (String) -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val language = languageByCode(code)
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 5
    }

    if (language == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Language not found",
                description = "We don't have that language yet. Browse the ones we do.",
                actionLabel = "Browse languages",
                onAction = onBack,
            )
        }
        return
    }

    val all = remember(books, code) { books.filter { it.language == code } }
    val sections = remember(all, language) {
        listOf(
            "Popular ${language.name} Books" to all.filter { "popular" in it.tags },
            "New ${language.name} Books" to all.sortedByDescending { it.added }.take(5),
            "Recommended ${language.name} Books" to all.filter { "recommended" in it.tags },
            "Most Read ${language.name} Books" to all.sortedByDescending { it.reviews }.take(5),
        ).filter { it.second.isNotEmpty() }
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
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to languages",
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = "${language.flag} ${language.name} Books",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (all.isEmpty()) {
                EmptyState(
                    title = "No Books Found",
                    description = "No titles in this language yet — try another one.",
                    actionLabel = "Browse languages",
                    onAction = onBack,
                )
            } else {
                sections.forEach { (title, list) ->
                    Column {
                        SectionHeader(title = title)
                        BookGrid(books = list, columns = columns, onBookClick = onBookClick)
                    }
                }
            }

            Column {
                SectionHeader(title = "Other languages")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LANGUAGES.filter { it.code != code }.forEach { l ->
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onLanguageClick(l.code) },
                            shape = CircleShape,
                            color = readingColors.card,
                            shadowElevation = 2.dp,
                        ) {
                            Text(
                                text = "${l.flag} ${l.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
