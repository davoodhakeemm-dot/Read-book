package com.example.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Tap empty background 3 times quickly (within 700ms) to exit the private messaging area
 * and return straight to the reading app.
 */
@Composable
fun TripleTapExitContainer(
    onTripleTapExit: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val taps = remember { mutableListOf<Long>() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onTripleTapExit) {
                detectTapGestures(
                    onTap = {
                        val now = System.currentTimeMillis()
                        taps.add(now)
                        taps.removeAll { now - it >= 700L }
                        if (taps.size >= 3) {
                            taps.clear()
                            onTripleTapExit()
                        }
                    },
                )
            },
    ) {
        content()
    }
}
