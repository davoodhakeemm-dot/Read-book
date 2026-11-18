package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.LibraryState
import com.example.model.categoryBySlug
import com.example.model.formatShortDate
import com.example.model.languageByCode
import com.example.model.progressPercent
import com.example.model.readingMinutes
import com.example.ui.components.BookCover
import com.example.ui.components.BookGrid
import com.example.ui.components.BrandButton
import com.example.ui.components.CoverSize
import com.example.ui.components.EmptyState
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalReadingColors
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    book: Book?,
    allBooks: List<Book>,
    libraryState: LibraryState,
    widthSizeClass: WindowWidthSizeClass,
    onBack: () -> Unit,
    onReadNow: (Book) -> Unit,
    onToggleFavorite: (String, (Boolean) -> Unit) -> Unit,
    onToggleBookmark: (String, Int, (Boolean) -> Unit) -> Unit,
    onCategoryClick: (String) -> Unit,
    onLanguageClick: (String) -> Unit,
    onBookClick: (Book) -> Unit,
    onExploreClick: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (book == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "Book not found",
                description = "This title may have been moved. Explore the library instead.",
                actionLabel = "Explore Books",
                onAction = onExploreClick,
            )
        }
        return
    }

    val cat = categoryBySlug(book.category)
    val lang = languageByCode(book.language)
    val isFav = book.id in libraryState.favorites
    val progress = libraryState.progress[book.id]
    val isBookmarked = libraryState.bookmarks.any { it.bookId == book.id }
    val similar = remember(allBooks, book) {
        allBooks.filter {
            it.id != book.id && (it.category == book.category || it.language == book.language)
        }.take(5)
    }

    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 5
    }
    val isWide = widthSizeClass != WindowWidthSizeClass.Compact

    fun shareBook() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, book.title)
            putExtra(Intent.EXTRA_TEXT, "Check out \"${book.title}\" by ${book.author} on Reading Corner!")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share book"))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isWide) 24.dp else 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 920.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .testTag("book_detail_back"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = "Book Details",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(
                        onClick = ::shareBook,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary)
                            .testTag("book_detail_share_top"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share book",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Cover & info section
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Box(modifier = Modifier.width(180.dp)) {
                            BookCover(book = book, size = CoverSize.LG)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            BookMetaAndActions(
                                book = book,
                                catName = cat?.let { "${it.icon} ${it.name}" },
                                langName = lang?.let { "${it.flag} ${it.name}" },
                                isFav = isFav,
                                isBookmarked = isBookmarked,
                                progressPage = progress?.page,
                                progressPages = progress?.pages,
                                onReadNow = { onReadNow(book) },
                                onAddToLibrary = {
                                    onToggleFavorite(book.id) { added ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (added) "Added to My Library" else "Removed from library",
                                            )
                                        }
                                    }
                                },
                                onToggleFavorite = { onToggleFavorite(book.id) {} },
                                onToggleBookmark = {
                                    onToggleBookmark(book.id, progress?.page ?: 0) {}
                                },
                                onShare = ::shareBook,
                                onCategoryClick = { onCategoryClick(book.category) },
                                onLanguageClick = { onLanguageClick(book.language) },
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(148.dp)
                                .align(Alignment.CenterHorizontally),
                        ) {
                            BookCover(book = book, size = CoverSize.LG)
                        }
                        BookMetaAndActions(
                            book = book,
                            catName = cat?.let { "${it.icon} ${it.name}" },
                            langName = lang?.let { "${it.flag} ${it.name}" },
                            isFav = isFav,
                            isBookmarked = isBookmarked,
                            progressPage = progress?.page,
                            progressPages = progress?.pages,
                            onReadNow = { onReadNow(book) },
                            onAddToLibrary = {
                                onToggleFavorite(book.id) { added ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (added) "Added to My Library" else "Removed from library",
                                        )
                                    }
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(book.id) {} },
                            onToggleBookmark = {
                                onToggleBookmark(book.id, progress?.page ?: 0) {}
                            },
                            onShare = ::shareBook,
                            onCategoryClick = { onCategoryClick(book.category) },
                            onLanguageClick = { onLanguageClick(book.language) },
                        )
                    }
                }

                // About This Book
                Column {
                    SectionHeader(title = "About This Book")
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = readingColors.card,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = book.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MetaItem("Publisher", book.publisher, Modifier.weight(1f))
                                MetaItem(
                                    "Published",
                                    if (book.year < 0) "${kotlin.math.abs(book.year)} BCE" else book.year.toString(),
                                    Modifier.weight(1f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                MetaItem("Pages", book.pageCount.toString(), Modifier.weight(1f))
                                MetaItem("Added", formatShortDate(book.added), Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (similar.isNotEmpty()) {
                    Column {
                        SectionHeader(title = "More Like This")
                        BookGrid(books = similar, columns = columns, onBookClick = onBookClick)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookMetaAndActions(
    book: Book,
    catName: String?,
    langName: String?,
    isFav: Boolean,
    isBookmarked: Boolean,
    progressPage: Int?,
    progressPages: Int?,
    onReadNow: () -> Unit,
    onAddToLibrary: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onCategoryClick: () -> Unit,
    onLanguageClick: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val reviewsFormatted = remember(book.reviews) {
        NumberFormat.getNumberInstance(Locale.US).format(book.reviews)
    }

    Column {
        Text(
            text = book.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = book.author,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(readingColors.gold.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = readingColors.gold,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = String.format(Locale.US, "%.1f", book.rating),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = readingColors.goldFg,
                )
                Text(
                    text = "($reviewsFormatted)",
                    fontSize = 11.sp,
                    color = readingColors.goldFg.copy(alpha = 0.7f),
                )
            }

            if (langName != null) {
                Text(
                    text = langName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .clickable(onClick = onLanguageClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            if (catName != null) {
                Text(
                    text = catName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = readingColors.accentFg,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(readingColors.accentBg)
                        .clickable(onClick = onCategoryClick)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }

            Text(
                text = "${book.pageCount} pages",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )

            Text(
                text = "~${readingMinutes(book)} min",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }

        if (progressPage != null && progressPages != null) {
            val pct = progressPercent(progressPage, progressPages)
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = readingColors.card,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Your progress",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "$pct% completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((pct.coerceIn(0, 100) / 100f))
                                .clip(CircleShape)
                                .background(readingColors.brandBrush),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BrandButton(
                text = if (progressPage != null) "Continue Reading" else "Read Now",
                onClick = onReadNow,
                modifier = Modifier
                    .weight(1f)
                    .testTag("read_now_button"),
            )
            Surface(
                modifier = Modifier
                    .height(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAddToLibrary)
                    .testTag("add_to_library_button"),
                shape = CircleShape,
                color = readingColors.card,
                border = BorderStroke(1.dp, readingColors.border),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Add to Library",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionPill(
                label = if (isFav) "Favorited" else "Favorite",
                icon = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                active = isFav,
                isDestructiveActive = true,
                onClick = onToggleFavorite,
                modifier = Modifier
                    .weight(1f)
                    .testTag("favorite_toggle_button"),
            )
            ActionPill(
                label = if (isBookmarked) "Bookmarked" else "Bookmark",
                icon = if (isBookmarked) Icons.Filled.BookmarkAdded else Icons.Outlined.BookmarkBorder,
                active = isBookmarked,
                isDestructiveActive = false,
                onClick = onToggleBookmark,
                modifier = Modifier
                    .weight(1f)
                    .testTag("bookmark_toggle_button"),
            )
            ActionPill(
                label = "Share",
                icon = Icons.Filled.Share,
                active = false,
                isDestructiveActive = false,
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .testTag("share_book_button"),
            )
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    isDestructiveActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readingColors = LocalReadingColors.current
    val bgColor = when {
        active && isDestructiveActive -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        active -> readingColors.sidebarAccent
        else -> readingColors.card
    }
    val fgColor = when {
        active && isDestructiveActive -> MaterialTheme.colorScheme.error
        active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, readingColors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
