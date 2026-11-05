package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UserProfileState
import com.example.model.AtmospherePreference
import com.example.model.BudgetTier
import com.example.model.ColorPaletteOption
import com.example.model.DecorDensity
import com.example.model.InteriorStyle
import com.example.model.RoomAnalysis
import com.example.model.RoomCatalog
import com.example.viewmodel.AnalysisUiState
import com.example.viewmodel.DesignStudioUiState
import com.example.viewmodel.GenerationUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignStudioScreen(
    studioState: DesignStudioUiState,
    userProfile: UserProfileState,
    onPickFromGallery: () -> Unit,
    onTakeCameraPhoto: () -> Unit,
    onLoadSamplePhoto: (Boolean) -> Unit,
    onRemovePhoto: () -> Unit,
    onRetryAnalysis: () -> Unit,
    onSelectStyle: (InteriorStyle) -> Unit,
    onSelectColorPalette: (ColorPaletteOption) -> Unit,
    onSelectBudget: (BudgetTier) -> Unit,
    onSelectAtmosphere: (AtmospherePreference) -> Unit,
    onSelectDecorDensity: (DecorDensity) -> Unit,
    onToggleFurniturePref: (String) -> Unit,
    onCustomInstructionsChange: (String) -> Unit,
    onAppendQuickInstruction: (String) -> Unit,
    onTransformRoom: () -> Unit,
    onLaunchDemoComparison: () -> Unit,
    onOpenPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "AI Belsőépítészeti Stúdió",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "1. Tölts fel fotót • 2. AI Szobaelemzés • 3. Stílus & Átalakítás",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Step 1: Photo Upload & Preview Card
            item {
                PhotoUploadCard(
                    studioState = studioState,
                    onPickFromGallery = onPickFromGallery,
                    onTakeCameraPhoto = onTakeCameraPhoto,
                    onLoadSamplePhoto = onLoadSamplePhoto,
                    onRemovePhoto = onRemovePhoto
                )
            }

            // Step 2: Gemini AI Room Analysis Section
            item {
                RoomAnalysisSectionCard(
                    hasPhoto = studioState.uploadedBitmap != null,
                    analysisState = studioState.analysisState,
                    onStartOrRetryAnalysis = onRetryAnalysis
                )
            }

            // Step 3: 12 Interior Design Styles Selection
            item {
                InteriorStyleSelectionSection(
                    styles = RoomCatalog.styles,
                    selectedStyle = studioState.selectedStyle,
                    isUserPremium = userProfile.isPremium,
                    onSelectStyle = { style ->
                        if (style.isPremiumOnly && !userProfile.isPremium) {
                            onSelectStyle(style)
                        } else {
                            onSelectStyle(style)
                        }
                    },
                    onOpenPremium = onOpenPremium
                )
            }

            // Step 4: Color Palette, Budget, Atmosphere, Decor Density & Furniture Preferences
            item {
                CustomizationParametersCard(
                    studioState = studioState,
                    onSelectColorPalette = onSelectColorPalette,
                    onSelectBudget = onSelectBudget,
                    onSelectAtmosphere = onSelectAtmosphere,
                    onSelectDecorDensity = onSelectDecorDensity,
                    onToggleFurniturePref = onToggleFurniturePref
                )
            }

            // Step 5: Custom Instructions & Quick Prompt Chips
            item {
                CustomPromptCard(
                    customInstructions = studioState.customInstructions,
                    onCustomInstructionsChange = onCustomInstructionsChange,
                    onAppendQuickInstruction = onAppendQuickInstruction
                )
            }

            // Step 6: Main "Szoba átalakítása" Action & Generation Status / Error / Demo Mode
            item {
                TransformActionSection(
                    studioState = studioState,
                    userProfile = userProfile,
                    onTransformRoom = onTransformRoom,
                    onLaunchDemoComparison = onLaunchDemoComparison,
                    onOpenPremium = onOpenPremium
                )
            }
        }
    }
}

@Composable
private fun PhotoUploadCard(
    studioState: DesignStudioUiState,
    onPickFromGallery: () -> Unit,
    onTakeCameraPhoto: () -> Unit,
    onLoadSamplePhoto: (Boolean) -> Unit,
    onRemovePhoto: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Szobafotó feltöltése",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Támogatott: JPG, JPEG, PNG, WebP (automatikus tömörítéssel)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (studioState.uploadedBitmap != null) {
                    IconButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.testTag("btn_remove_uploaded_photo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Feltöltött kép eltávolítása",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (studioState.isCompressingPhoto) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Kép ellenőrzése és optimalizálása...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (studioState.uploadedBitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Image(
                        bitmap = studioState.uploadedBitmap.asImageBitmap(),
                        contentDescription = "Feltöltött szobafotó",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (studioState.isUploadedDemoPhoto) {
                                "MINTA SZOBAFOTÓ (DEMÓ / TESZT)"
                            } else {
                                "SAJÁT FELTÖLTÖTT FOTÓ (${studioState.uploadedBitmap.width}×${studioState.uploadedBitmap.height})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickFromGallery,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Másik fotó")
                    }
                    OutlinedButton(
                        onClick = onTakeCameraPhoto,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Új fotó kamerával")
                    }
                }
            } else {
                // Empty state upload box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onPickFromGallery() }
                        .padding(22.dp)
                        .testTag("photo_dropzone_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Fotó kiválasztása",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tölts fel egy fotót a szobádról",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Válassz a galériából vagy készíts új felvételt a kamerával.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onPickFromGallery,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_pick_gallery"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galéria")
                            }

                            OutlinedButton(
                                onClick = onTakeCameraPhoto,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_take_camera"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kamera")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = { onLoadSamplePhoto(false) },
                            modifier = Modifier.testTag("btn_load_sample_room")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nincs most fotód? Minta nappali betöltése teszteléshez")
                        }
                    }
                }
            }

            AnimatedVisibility(visible = studioState.uploadError != null) {
                studioState.uploadError?.let { errorMsg ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomAnalysisSectionCard(
    hasPhoto: Boolean,
    analysisState: AnalysisUiState,
    onStartOrRetryAnalysis: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "2",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gemini AI Szobaelemzés",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Építészeti adottságok, fényviszonyok és becsült méretek",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hasPhoto) {
                    IconButton(
                        onClick = onStartOrRetryAnalysis,
                        modifier = Modifier.testTag("btn_refresh_room_analysis")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Szobaelemzés újrafuttatása",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (analysisState) {
                is AnalysisUiState.Idle -> {
                    if (!hasPhoto) {
                        Text(
                            text = "Tölts fel egy szobafotót az automatikus Gemini AI tér- és bútoranalízishez.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        OutlinedButton(
                            onClick = onStartOrRetryAnalysis,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Szoba elemzése Gemini AI-val")
                        }
                    }
                }

                is AnalysisUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "A Gemini 3.8 Flash elemzi a szoba adottságait...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                is AnalysisUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = analysisState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onStartOrRetryAnalysis,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Elemzés újrapróbálása")
                        }
                    }
                }

                is AnalysisUiState.Success -> {
                    val analysis = analysisState.analysis
                    RoomAnalysisDetailContent(
                        analysis = analysis,
                        onRunLiveAi = if (analysis.isDemoSample) onStartOrRetryAnalysis else null
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomAnalysisDetailContent(
    analysis: RoomAnalysis,
    onRunLiveAi: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = analysis.roomType,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Surface(
                color = if (analysis.isDemoSample) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = if (analysis.isDemoSample) "MINTA ELEMZÉS" else "VALÓS GEMINI AI ELEMZÉS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        AnalysisRowItem(
            label = "Becsült méretek (Becslés):",
            value = analysis.estimatedDimensions
        )
        AnalysisRowItem(
            label = "Meglévő bútorzat:",
            value = analysis.existingFurniture.joinToString(" • ")
        )
        AnalysisRowItem(
            label = "Falak színe:",
            value = analysis.wallColors
        )
        AnalysisRowItem(
            label = "Padlóburkolat:",
            value = analysis.flooring
        )
        AnalysisRowItem(
            label = "Nyílászárók (Ablakok / Ajtók):",
            value = analysis.windowsAndDoors
        )
        AnalysisRowItem(
            label = "Fényviszonyok:",
            value = analysis.lightingConditions
        )
        AnalysisRowItem(
            label = "Jelenlegi térelrendezés:",
            value = analysis.existingLayout
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "AI Belsőépítész javaslata:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = analysis.designRecommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Mandatory explicit disclaimer that ordinary photographs only allow visual estimation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "A méretek hagyományos fénykép alapján készült vizuális becslések, nem pontos építészeti mérések.",
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onRunLiveAi != null) {
            TextButton(onClick = onRunLiveAi) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Valós idejű Gemini AI elemzés futtatása ezen a képen")
            }
        }
    }
}

@Composable
private fun AnalysisRowItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InteriorStyleSelectionSection(
    styles: List<InteriorStyle>,
    selectedStyle: InteriorStyle,
    isUserPremium: Boolean,
    onSelectStyle: (InteriorStyle) -> Unit,
    onOpenPremium: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "3",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Belsőépítészeti stílus kiválasztása (12 stílus)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kiválasztva: ${selectedStyle.nameHu}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2-row horizontal grid or cards for all 12 styles
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(styles, key = { it.id }) { style ->
                    val isSelected = style.id == selectedStyle.id
                    Card(
                        modifier = Modifier
                            .width(195.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { onSelectStyle(style) }
                            .testTag("style_option_${style.id}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = style.previewResId),
                                    contentDescription = style.nameHu,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Kiválasztva",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                if (style.isPremiumOnly && !isUserPremium) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "PRO",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = style.nameHu,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = style.shortDescriptionHu,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomizationParametersCard(
    studioState: DesignStudioUiState,
    onSelectColorPalette: (ColorPaletteOption) -> Unit,
    onSelectBudget: (BudgetTier) -> Unit,
    onSelectAtmosphere: (AtmospherePreference) -> Unit,
    onSelectDecorDensity: (DecorDensity) -> Unit,
    onToggleFurniturePref: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Színpaletta, Költségvetés és Hangulat",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Color Palette Selector
            Column {
                Text(
                    text = "Színpaletta: ${studioState.selectedColorPalette.nameHu}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(RoomCatalog.colorPalettes, key = { it.id }) { palette ->
                        val selected = palette.id == studioState.selectedColorPalette.id
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectColorPalette(palette) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    palette.colors.forEach { swatchColor ->
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(0.5.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = palette.nameHu,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. Budget Category (Low / Medium / Premium)
            Column {
                Text(
                    text = "Költségvetési kategória",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetTier.entries.forEach { tier ->
                        val isSelected = tier == studioState.selectedBudget
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.8.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectBudget(tier) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tier.labelHu,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tier.subtitleHu,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Kiválasztva",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 3. Bright vs Dark Atmosphere & Minimal vs Decorative
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hangulat és fény",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AtmospherePreference.entries.forEach { atm ->
                        FilterChip(
                            selected = studioState.selectedAtmosphere == atm,
                            onClick = { onSelectAtmosphere(atm) },
                            label = { Text(atm.labelHu, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dekoráció sűrűsége",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DecorDensity.entries.forEach { density ->
                        FilterChip(
                            selected = studioState.selectedDecorDensity == density,
                            onClick = { onSelectDecorDensity(density) },
                            label = { Text(density.labelHu, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 4. Furniture Preferences Multi-Select Chips
            Column {
                Text(
                    text = "Bútor és berendezési preferenciák",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoomCatalog.furniturePreferences.forEach { pref ->
                        val selected = studioState.selectedFurniturePreferences.contains(pref)
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleFurniturePref(pref) },
                            label = { Text(pref) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomPromptCard(
    customInstructions: String,
    onCustomInstructionsChange: (String) -> Unit,
    onAppendQuickInstruction: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Egyedi utasítások az AI számára (Opcionális)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Írd le szabadon, mit szeretnél megváltoztatni vagy megtartani a szobában:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = customInstructions,
                onValueChange = onCustomInstructionsChange,
                placeholder = {
                    Text("Pl.: Legyen világosabb a szoba, használj több természetes tölgyfát, adj hozzá egy nagy kényelmes kanapét...")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_instructions_input"),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Gyors javaslatok beszúrása:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RoomCatalog.quickCustomPrompts.forEach { promptSnippet ->
                    AssistChip(
                        onClick = { onAppendQuickInstruction(promptSnippet) },
                        label = {
                            Text(
                                text = promptSnippet,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransformActionSection(
    studioState: DesignStudioUiState,
    userProfile: UserProfileState,
    onTransformRoom: () -> Unit,
    onLaunchDemoComparison: () -> Unit,
    onOpenPremium: () -> Unit
) {
    val isGenerating = studioState.generationState is GenerationUiState.Loading

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Generation Loading Banner
        if (studioState.generationState is GenerationUiState.Loading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "AI Látványterv Generálása Folyamatban...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = studioState.generationState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Generation Error Banner with Retry & Demo Fallback
        if (studioState.generationState is GenerationUiState.Error) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Képgenerálási hiba történt",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = studioState.generationState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onTransformRoom,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Újrapróbálkozás")
                        }
                        OutlinedButton(
                            onClick = onLaunchDemoComparison,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Minta látványterv megtekintése")
                        }
                    }
                }
            }
        }

        // Main "Szoba átalakítása" Button
        Button(
            onClick = onTransformRoom,
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("btn_transform_room"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Szoba átalakítása (${studioState.selectedStyle.nameHu})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Prototype / Demo Showcase button clearly labeled
        OutlinedButton(
            onClick = onLaunchDemoComparison,
            enabled = !isGenerating,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_demo_showcase"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Prototípus Demó Mód: Azonnali Minta Látványterv",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Text(
            text = if (userProfile.isPremium) {
                "Prémium előfizetés aktív: Korlátlan AI generálás engedélyezve."
            } else {
                "Ingyenes csomag: ${userProfile.remainingFreeGenerations} / ${userProfile.freeGenerationLimit} ingyenes AI képgenerálás maradt."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
