package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.BeforeAfterSlider
import com.example.util.ImageUtils
import com.example.viewmodel.DesignStudioUiState
import com.example.viewmodel.GenerationUiState

private enum class ComparisonViewMode(val labelHu: String) {
    SPLIT_SLIDER("Összehasonlítás"),
    BEFORE_ONLY("Előtte (Eredeti)"),
    AFTER_ONLY("Utána (AI Terv)")
}

@Composable
fun ComparisonScreen(
    studioState: DesignStudioUiState,
    onBack: () -> Unit,
    onSaveToDevice: () -> Unit,
    onShareImage: () -> Unit,
    onCustomInstructionsChange: (String) -> Unit,
    onRegenerateWithAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var viewMode by remember { mutableStateOf(ComparisonViewMode.SPLIT_SLIDER) }

    val beforeBitmap: Bitmap = studioState.activeComparisonOriginal ?: remember {
        ImageUtils.loadDrawableBitmap(context, R.drawable.img_hero_living_before_1793839689397)
    }
    val afterBitmap: Bitmap = studioState.activeComparisonGenerated ?: remember {
        ImageUtils.loadDrawableBitmap(context, R.drawable.img_hero_living_after_1793839687876)
    }

    val isGenerating = studioState.generationState is GenerationUiState.Loading

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Top Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("comparison_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Vissza"
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = studioState.activeComparisonTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Stílus: ${studioState.activeComparisonStyleName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = if (studioState.activeComparisonIsDemo) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (studioState.activeComparisonIsDemo) "MINTA DEMÓ" else "VALÓS AI TERV",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // View Mode Selector (Összehasonlítás | Előtte | Utána)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ComparisonViewMode.entries.forEach { mode ->
                        val selected = viewMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { viewMode = mode },
                            label = {
                                Text(
                                    text = mode.labelHu,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Interactive Comparison Slider or Single Image View
            item {
                when (viewMode) {
                    ComparisonViewMode.SPLIT_SLIDER -> {
                        Column {
                            BeforeAfterSlider(
                                beforeBitmap = beforeBitmap,
                                afterBitmap = afterBitmap,
                                beforeLabel = "ELŐTTE (EREDETI)",
                                afterLabel = "UTÁNA (${studioState.activeComparisonStyleName.uppercase()})"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Húzd a középső csúszkát balra vagy jobbra az eredeti és az átalakított szoba összehasonlításához!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ComparisonViewMode.BEFORE_ONLY -> {
                        val ratio = (beforeBitmap.width.toFloat() / beforeBitmap.height.toFloat().coerceAtLeast(1f))
                            .coerceIn(0.75f, 1.8f)
                        Image(
                            bitmap = beforeBitmap.asImageBitmap(),
                            contentDescription = "Eredeti szobafotó",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }

                    ComparisonViewMode.AFTER_ONLY -> {
                        val ratio = (afterBitmap.width.toFloat() / afterBitmap.height.toFloat().coerceAtLeast(1f))
                            .coerceIn(0.75f, 1.8f)
                        Image(
                            bitmap = afterBitmap.asImageBitmap(),
                            contentDescription = "AI által újratervezett szobafotó",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                }
            }

            // Save & Share Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSaveToDevice,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_save_generated_image"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mentés",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onShareImage,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("btn_share_generated_image"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Megosztás",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Regenerate with custom instructions card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Új verzió generálása módosított utasítással",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Finomhangold az eredményt (pl. „Legyen világosabb”, „Adj hozzá több növényt”):",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = studioState.customInstructions,
                            onValueChange = onCustomInstructionsChange,
                            placeholder = { Text("Írd be az új módosítási kérést...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isGenerating) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Új verzió generálása folyamatban (30-60 mp)...")
                            }
                        } else {
                            Button(
                                onClick = onRegenerateWithAi,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_regenerate_design"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Új verzió generálása Gemini AI-val")
                            }
                        }
                    }
                }
            }
        }
    }
}
