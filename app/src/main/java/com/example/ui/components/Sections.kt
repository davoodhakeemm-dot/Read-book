package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.model.CATEGORIES
import com.example.model.LANGUAGES
import com.example.ui.theme.LocalReadingColors

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onViewAll: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onViewAll != null) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onViewAll)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun BookScroller(
    books: List<Book>,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                book = book,
                onClick = { onBookClick(book) },
                showMeta = false,
                modifier = Modifier.width(128.dp),
            )
        }
    }
}

@Composable
fun BookGrid(
    books: List<Book>,
    columns: Int,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = books.chunked(columns.coerceAtLeast(1))
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { book ->
                    BookCard(
                        book = book,
                        onClick = { onBookClick(book) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat((columns - rowItems.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CategoryChips(
    columns: Int,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    limit: Int? = null,
) {
    val readingColors = LocalReadingColors.current
    val items = if (limit != null) CATEGORIES.take(limit) else CATEGORIES
    val rows = items.chunked(columns.coerceAtLeast(1))

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { cat ->
                    val bgModifier = if (readingColors.isDark) {
                        Modifier.background(readingColors.card)
                    } else {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(Color(cat.tintFromHex), Color(cat.tintToHex)),
                            ),
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onCategoryClick(cat.slug) }
                            .testTag("category_chip_${cat.slug}"),
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 2.dp,
                        color = Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier
                                .then(bgModifier)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(readingColors.card.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = cat.icon, fontSize = 18.sp)
                            }
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                repeat((columns - rowItems.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun LanguageChips(
    columns: Int,
    onLanguageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    limit: Int? = null,
) {
    val readingColors = LocalReadingColors.current
    val items = if (limit != null) LANGUAGES.take(limit) else LANGUAGES
    val rows = items.chunked(columns.coerceAtLeast(1))

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { lang ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onLanguageClick(lang.code) }
                            .testTag("language_chip_${lang.code}"),
                        shape = RoundedCornerShape(16.dp),
                        color = readingColors.card,
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(text = lang.flag, fontSize = 18.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = lang.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = lang.nativeName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                repeat((columns - rowItems.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readingColors = LocalReadingColors.current
    val bgModifier = if (selected) {
        Modifier.background(readingColors.brandBrush)
    } else {
        Modifier.background(MaterialTheme.colorScheme.secondary)
    }
    Box(
        modifier = modifier
            .heightIn(min = 38.dp)
            .clip(CircleShape)
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
            maxLines = 1,
        )
    }
}

@Composable
fun BrandButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val readingColors = LocalReadingColors.current
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(
                brush = readingColors.brandBrush,
                alpha = if (enabled) 1f else 0.4f,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 1,
        )
    }
}
