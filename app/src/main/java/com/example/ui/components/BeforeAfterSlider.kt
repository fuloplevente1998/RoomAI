package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Interactive Before/After comparison slider component.
 * Displays the AI-redesigned image on the right and clips the original photograph on the left
 * according to [sliderPosition] (0.0f .. 1.0f).
 */
@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier,
    initialFraction: Float = 0.5f,
    beforeLabel: String = "ELŐTTE",
    afterLabel: String = "UTÁNA (AI)"
) {
    var sliderFraction by remember { mutableFloatStateOf(initialFraction.coerceIn(0.05f, 0.95f)) }
    var componentWidthPx by remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current

    val aspectRatio = remember(afterBitmap) {
        val ratio = afterBitmap.width.toFloat() / afterBitmap.height.toFloat().coerceAtLeast(1f)
        ratio.coerceIn(0.75f, 1.8f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            )
            .onSizeChanged { size ->
                if (size.width > 0) {
                    componentWidthPx = size.width.toFloat()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (componentWidthPx > 0f) {
                        sliderFraction = (offset.x / componentWidthPx).coerceIn(0.03f, 0.97f)
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    if (componentWidthPx > 0f) {
                        sliderFraction = (change.position.x / componentWidthPx).coerceIn(0.03f, 0.97f)
                    }
                }
            }
            .testTag("before_after_slider")
    ) {
        // 1. Bottom Layer: AFTER (Redesigned by AI)
        Image(
            bitmap = afterBitmap.asImageBitmap(),
            contentDescription = afterLabel,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Clipped Layer: BEFORE (Original Room Photo)
        Image(
            bitmap = beforeBitmap.asImageBitmap(),
            contentDescription = beforeLabel,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val clipWidth = size.width * sliderFraction
                    clipRect(
                        left = 0f,
                        top = 0f,
                        right = clipWidth,
                        bottom = size.height
                    ) {
                        this@drawWithContent.drawContent()
                    }
                }
        )

        // 3. Labels in corners
        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = beforeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
        ) {
            Text(
                text = afterLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // 4. Vertical divider line + circular draggable handle
        val lineOffsetPx = (componentWidthPx * sliderFraction).roundToInt()
        val handleRadiusPx = with(density) { 22.dp.toPx() }.roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(x = lineOffsetPx - 1, y = 0) }
                .width(2.5.dp)
                .fillMaxHeight()
                .background(Color.White)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(x = lineOffsetPx - handleRadiusPx, y = 0) }
                .size(44.dp)
                .shadow(8.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "Csúszka mozgatása",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
