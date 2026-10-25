package com.example.terminallauncher.ui

import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun MatrixRainScreen(
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val matrixChars = remember {
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ@#$%&*+<>?/\\"
    }

    val columnCount = 28
    val drops = remember {
        IntArray(columnCount) { Random.nextInt(-20, 0) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "matrix_rain")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = InfiniteRepeatableSpec(
            animation = tween(durationMillis = 5000, easing = LinearEasing)
        ),
        label = "matrix_step"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D0A))
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Enter || event.key == Key.Escape || event.key == Key.Back) {
                    onDismiss()
                    true
                } else {
                    onDismiss()
                    true
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onDismiss()
            }
    ) {
        val paint = remember {
            android.graphics.Paint().apply {
                color = android.graphics.Color.GREEN
                textSize = 42f
                typeface = android.graphics.Typeface.MONOSPACE
                isAntiAlias = true
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val columnWidth = canvasWidth / columnCount
            
            // Re-trigger animation frame rendering
            val frame = animationProgress

            for (i in 0 until columnCount) {
                val x = i * columnWidth
                val dropStep = (drops[i] + frame.toInt()) % 30
                val y = dropStep * 48f

                if (y > canvasHeight && Random.nextFloat() > 0.95f) {
                    drops[i] = 0
                }

                val charIndex = Random.nextInt(matrixChars.length)
                val charToDraw = matrixChars[charIndex].toString()

                // Lead character glow
                paint.color = android.graphics.Color.WHITE
                drawContext.canvas.nativeCanvas.drawText(charToDraw, x, y, paint)

                // Trail green character
                if (drops[i] > 1) {
                    paint.color = 0xFF00FF66.toInt()
                    val prevChar = matrixChars[Random.nextInt(matrixChars.length)].toString()
                    drawContext.canvas.nativeCanvas.drawText(prevChar, x, y - 48f, paint)
                }
            }
        }

        Text(
            text = "[ MATRIX MODE - PRESS ENTER OR TAP TO EXIT ]",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = Color(0xFF00FF66)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
