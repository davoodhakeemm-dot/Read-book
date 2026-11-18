package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Book
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.parseHexColor

enum class CoverSize {
    SM,
    MD,
    LG,
}

@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    size: CoverSize = CoverSize.MD,
) {
    val fromColor = remember(book.cover.from) { parseHexColor(book.cover.from) }
    val toColor = remember(book.cover.to) { parseHexColor(book.cover.to) }
    val inkColor = remember(book.cover.ink) { parseHexColor(book.cover.ink, Color.White) }

    val cornerRadius = when (size) {
        CoverSize.SM -> 8.dp
        CoverSize.MD -> 12.dp
        CoverSize.LG -> 16.dp
    }
    val padding = when (size) {
        CoverSize.SM -> 6.dp
        CoverSize.MD -> 12.dp
        CoverSize.LG -> 16.dp
    }
    val titleSize = when (size) {
        CoverSize.SM -> 9.sp
        CoverSize.MD -> 13.sp
        CoverSize.LG -> 18.sp
    }
    val titleLineHeight = when (size) {
        CoverSize.SM -> 11.sp
        CoverSize.MD -> 17.sp
        CoverSize.LG -> 23.sp
    }
    val authorSize = when (size) {
        CoverSize.SM -> 7.sp
        CoverSize.MD -> 9.sp
        CoverSize.LG -> 11.sp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .shadow(
                elevation = if (size == CoverSize.SM) 4.dp else 10.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0x66221933),
                spotColor = Color(0x66221933),
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(fromColor, toColor),
                    start = Offset.Zero,
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            ),
    ) {
        // Left spine shadow band (~7% width)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.075f)
                .background(Color.Black.copy(alpha = 0.22f)),
        )

        // Subtle top-right highlight orb
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-32).dp)
                .size(if (size == CoverSize.SM) 42.dp else 90.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = if (size == CoverSize.SM) 4.dp else 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = book.title,
                color = inkColor,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = titleSize,
                lineHeight = titleLineHeight,
                maxLines = if (size == CoverSize.SM) 3 else 4,
                overflow = TextOverflow.Ellipsis,
            )

            if (size != CoverSize.SM) {
                Column {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(inkColor.copy(alpha = 0.5f)),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = book.author.uppercase(),
                        color = inkColor.copy(alpha = 0.85f),
                        fontSize = authorSize,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
