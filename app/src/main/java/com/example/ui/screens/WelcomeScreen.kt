package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.BrandButton
import com.example.ui.components.SecretLogoTrigger
import com.example.ui.theme.LocalReadingColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun WelcomeScreen(
    onStartReading: () -> Unit,
    onExploreBooks: () -> Unit,
    onContinueHome: () -> Unit,
    onSecretUnlock: () -> Unit,
) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1900L)
        showSplash = false
    }

    if (showSplash) {
        SplashContent()
    } else {
        WelcomeContent(
            onStartReading = onStartReading,
            onExploreBooks = onExploreBooks,
            onContinueHome = onContinueHome,
            onSecretUnlock = onSecretUnlock,
        )
    }
}

@Composable
private fun SplashContent() {
    val readingColors = LocalReadingColors.current
    val infinite = rememberInfiniteTransition(label = "splash_loader")
    val offsetFraction by infinite.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "splash_bar",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readingColors.nightBrush)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = "Reading Corner logo",
                tint = Color.White,
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Reading\nCorner",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "READ • EXPLORE • IMAGINE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.6.sp,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(36.dp))
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .fillMaxHeight()
                        .offset { IntOffset((offsetFraction * 160.dp.toPx()).roundToInt(), 0) }
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f)),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Loading your library…",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun WelcomeContent(
    onStartReading: () -> Unit,
    onExploreBooks: () -> Unit,
    onContinueHome: () -> Unit,
    onSecretUnlock: () -> Unit,
) {
    val readingColors = LocalReadingColors.current
    var holdProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(readingColors.heroBrush),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SecretLogoTrigger(
                onTap = {},
                onSecretUnlock = onSecretUnlock,
                onHoldProgress = { holdProgress = it },
                modifier = Modifier.testTag("welcome_logo"),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = "Reading Corner",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp),
                    )
                    Text(
                        text = "READING CORNER",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(id = R.drawable.img_hero_books),
                contentDescription = "A stack of colourful books beside an open book and a warm cup of coffee",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .shadow(14.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Welcome to Reading Corner",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Discover stories, knowledge and new worlds.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BrandButton(
                    text = "Start Reading",
                    onClick = onStartReading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_reading_button"),
                )

                OutlinedButton(
                    onClick = onExploreBooks,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("explore_books_button"),
                ) {
                    Text(
                        text = "Explore Books",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Already reading with us? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onContinueHome)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("continue_home_link"),
                )
            }
        }

        if (holdProgress >= 0.45f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .fillMaxWidth(holdProgress)
                    .height(2.5.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
            )
        }
    }
}
