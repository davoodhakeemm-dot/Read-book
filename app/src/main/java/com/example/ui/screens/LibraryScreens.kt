package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.LibraryState
import com.example.model.formatShortDate
import com.example.model.progressPercent
import com.example.ui.components.BookGrid
import com.example.ui.components.BookRow
import com.example.ui.components.EmptyState
import com.example.ui.components.PageHeader
import com.example.ui.components.PillChip
import com.example.ui.theme.LocalReadingColors

private val LIBRARY_TABS = listOf("Currently Reading", "Favorites", "Bookmarks", "Completed")

@Composable
fun LibraryScreen(
    books: List<Book>,
    libraryState: LibraryState,
    widthSizeClass: WindowWidthSizeClass,
    onBookClick: (Book) -> Unit,
    onResumeBook: (Book, Int) -> Unit,
    onExploreClick: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    var activeTab by rememberSaveable { mutableStateOf("Currently Reading") }
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 5
    }

    val reading = remember(books, libraryState.progress, libraryState.completed) {
        libraryState.progress.entries
            .filter { it.key !in libraryState.completed }
            .sortedByDescending { it.value.updatedAt }
            .mapNotNull { entry ->
                val b = books.find { it.id == entry.key }
                if (b != null) b to entry.value else null
            }
    }
    val favorites = remember(books, libraryState.favorites) {
        libraryState.favorites.mapNotNull { id -> books.find { it.id == id } }
    }
    val completed = remember(books, libraryState.completed) {
        libraryState.completed.mapNotNull { id -> books.find { it.id == id } }
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
            PageHeader(title = "My Library", subtitle = "Everything you've saved and started.")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LIBRARY_TABS.forEach { tab ->
                    PillChip(
                        label = tab,
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                    )
                }
            }

            when (activeTab) {
                "Currently Reading" -> {
                    if (reading.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            reading.forEach { (book, p) ->
                                BookRow(
                                    book = book,
                                    percent = progressPercent(p.page, p.pages),
                                    onClick = { onBookClick(book) },
                                    trailing = {
                                        Row(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(readingColors.brandBrush)
                                                .clickable { onResumeBook(book, p.page) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = "Resume ${book.title} on page ${p.page + 1}",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = "p.${p.page + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    } else {
                        EmptyState(
                            title = "Nothing in progress",
                            description = "Start a book and it will show up here so you can pick up where you left off.",
                            actionLabel = "Explore books",
                            onAction = onExploreClick,
                        )
                    }
                }

                "Favorites" -> {
                    if (favorites.isNotEmpty()) {
                        BookGrid(books = favorites, columns = columns, onBookClick = onBookClick)
                    } else {
                        EmptyState(
                            title = "No favorites yet",
                            description = "Tap the heart on any book to keep it close at hand.",
                            actionLabel = "Find something to love",
                            onAction = onExploreClick,
                        )
                    }
                }

                "Completed" -> {
                    if (completed.isNotEmpty()) {
                        BookGrid(books = completed, columns = columns, onBookClick = onBookClick)
                    } else {
                        EmptyState(
                            title = "No finished books yet",
                            description = "Books you read to the last page land here as a record of your journey.",
                            actionLabel = "Start reading",
                            onAction = onExploreClick,
                        )
                    }
                }

                "Bookmarks" -> {
                    if (libraryState.bookmarks.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            libraryState.bookmarks.forEach { bm ->
                                val book = books.find { it.id == bm.bookId }
                                if (book != null) {
                                    BookRow(
                                        book = book,
                                        onClick = { onBookClick(book) },
                                        trailing = {
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(readingColors.brandBrush)
                                                    .clickable { onResumeBook(book, bm.page) }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                            ) {
                                                Text(
                                                    text = "Open p.${bm.page + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        EmptyState(
                            title = "No bookmarks",
                            description = "While reading, tap the bookmark icon to save a page for later.",
                            actionLabel = "Open a book",
                            onAction = onExploreClick,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun FavoritesScreen(
    books: List<Book>,
    libraryState: LibraryState,
    widthSizeClass: WindowWidthSizeClass,
    onBookClick: (Book) -> Unit,
    onExploreClick: () -> Unit,
) {
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 3
        else -> 5
    }
    val favorites = remember(books, libraryState.favorites) {
        libraryState.favorites.mapNotNull { id -> books.find { it.id == id } }
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
                title = "Favorites",
                subtitle = "${favorites.size} saved book${if (favorites.size == 1) "" else "s"}",
            )
            if (favorites.isNotEmpty()) {
                BookGrid(books = favorites, columns = columns, onBookClick = onBookClick)
            } else {
                EmptyState(
                    title = "No favorites yet",
                    description = "Tap the heart on any book and it will appear here.",
                    actionLabel = "Explore books",
                    onAction = onExploreClick,
                )
            }
        }
    }
}

@Composable
fun HistoryScreen(
    books: List<Book>,
    libraryState: LibraryState,
    widthSizeClass: WindowWidthSizeClass,
    onBookClick: (Book) -> Unit,
    onExploreClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (widthSizeClass == WindowWidthSizeClass.Compact) 16.dp else 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PageHeader(
                title = "Reading History",
                subtitle = "Your most recent reading sessions.",
            )
            if (libraryState.history.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    libraryState.history.forEach { h ->
                        val book = books.find { it.id == h.bookId }
                        if (book != null) {
                            val p = libraryState.progress[h.bookId]
                            BookRow(
                                book = book,
                                percent = progressPercent(p?.page ?: 0, p?.pages ?: 1),
                                onClick = { onBookClick(book) },
                                trailing = {
                                    Text(
                                        text = formatShortDate(h.at),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }
            } else {
                EmptyState(
                    title = "No history yet",
                    description = "Open a book and your reading activity will be listed here.",
                    actionLabel = "Start reading",
                    onAction = onExploreClick,
                )
            }
        }
    }
}
