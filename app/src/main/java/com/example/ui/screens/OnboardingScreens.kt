package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CATEGORIES
import com.example.model.LANGUAGES
import com.example.ui.components.BrandButton
import com.example.ui.theme.LocalReadingColors

@Composable
fun LanguageOnboardingScreen(
    selectedLanguages: List<String>,
    widthSizeClass: WindowWidthSizeClass,
    onToggleLanguage: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val columns = if (widthSizeClass == WindowWidthSizeClass.Compact) 3 else 4

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                shadowElevation = 6.dp,
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(thickness = 0.5.dp, color = readingColors.border)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val count = selectedLanguages.size
                        BrandButton(
                            text = if (count > 0) "Continue ($count)" else "Continue",
                            enabled = count > 0,
                            onClick = onContinue,
                            modifier = Modifier
                                .widthIn(max = 560.dp)
                                .fillMaxWidth()
                                .testTag("onboarding_lang_continue"),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
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
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Choose Your Language",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select one or more languages",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LANGUAGES.chunked(columns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEach { lang ->
                            val active = lang.code in selectedLanguages
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onToggleLanguage(lang.code) }
                                    .testTag("lang_option_${lang.code}"),
                                shape = RoundedCornerShape(18.dp),
                                color = if (active) readingColors.sidebarAccent else readingColors.card,
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                ),
                                shadowElevation = 2.dp,
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                                    if (active) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(readingColors.brandBrush),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(12.dp),
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(text = lang.flag, fontSize = 26.sp)
                                        Text(
                                            text = lang.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
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
    }
}

@Composable
fun CategoryOnboardingScreen(
    selectedCategories: List<String>,
    widthSizeClass: WindowWidthSizeClass,
    onToggleCategory: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    val columns = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 3
        WindowWidthSizeClass.Medium -> 4
        else -> 5
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.96f),
                shadowElevation = 6.dp,
            ) {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    HorizontalDivider(thickness = 0.5.dp, color = readingColors.border)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val count = selectedCategories.size
                        BrandButton(
                            text = if (count > 0) "Continue ($count)" else "Continue",
                            enabled = count > 0,
                            onClick = onContinue,
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .testTag("onboarding_cat_continue"),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
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
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "What would you like to read?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select one or more categories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(26.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CATEGORIES.chunked(columns).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEach { cat ->
                            val active = cat.slug in selectedCategories
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
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onToggleCategory(cat.slug) }
                                    .testTag("cat_option_${cat.slug}"),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Transparent,
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = if (active) MaterialTheme.colorScheme.primary else Color.Transparent,
                                ),
                                shadowElevation = 2.dp,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(bgModifier)
                                        .padding(8.dp),
                                ) {
                                    if (active) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(readingColors.brandBrush),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(12.dp),
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(text = cat.icon, fontSize = 26.sp)
                                        Text(
                                            text = cat.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
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
    }
}
