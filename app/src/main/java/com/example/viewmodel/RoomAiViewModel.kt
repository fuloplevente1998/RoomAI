package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.DesignProjectEntity
import com.example.data.DesignRepository
import com.example.data.GeminiRoomAiService
import com.example.data.RoomAiDatabase
import com.example.data.UserPreferencesRepository
import com.example.data.UserProfileState
import com.example.model.AppLanguage
import com.example.model.AtmospherePreference
import com.example.model.BudgetTier
import com.example.model.ChatMessage
import com.example.model.ColorPaletteOption
import com.example.model.DecorDensity
import com.example.model.InteriorStyle
import com.example.model.RoomAnalysis
import com.example.model.RoomCatalog
import com.example.model.SubscriptionPlan
import com.example.util.ImageUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "RoomAiViewModel"

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Success(val analysis: RoomAnalysis) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

sealed interface GenerationUiState {
    data object Idle : GenerationUiState
    data class Loading(val statusMessage: String) : GenerationUiState
    data class Success(
        val originalBitmap: Bitmap,
        val generatedBitmap: Bitmap,
        val isDemoSample: Boolean,
        val savedProjectId: Long? = null
    ) : GenerationUiState
    data class Error(val message: String) : GenerationUiState
}

data class DesignStudioUiState(
    val uploadedBitmap: Bitmap? = null,
    val isUploadedDemoPhoto: Boolean = false,
    val uploadError: String? = null,
    val isCompressingPhoto: Boolean = false,
    val selectedStyle: InteriorStyle = RoomCatalog.styles.first(),
    val selectedColorPalette: ColorPaletteOption = RoomCatalog.colorPalettes.first(),
    val selectedBudget: BudgetTier = BudgetTier.MEDIUM,
    val selectedAtmosphere: AtmospherePreference = AtmospherePreference.BRIGHT,
    val selectedDecorDensity: DecorDensity = DecorDensity.DECORATIVE,
    val selectedFurniturePreferences: Set<String> = setOf("Nagy, kényelmes kanapé", "Természetes tölgyfa bútorok"),
    val customInstructions: String = "",
    val analysisState: AnalysisUiState = AnalysisUiState.Idle,
    val generationState: GenerationUiState = GenerationUiState.Idle,
    val activeComparisonOriginal: Bitmap? = null,
    val activeComparisonGenerated: Bitmap? = null,
    val activeComparisonTitle: String = "Skandináv Nappali Átalakítás",
    val activeComparisonStyleName: String = "Skandináv",
    val activeComparisonIsDemo: Boolean = false,
    val snackbarMessage: String? = null
)

class RoomAiViewModel(
    private val appContext: Context,
    private val designRepository: DesignRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiService: GeminiRoomAiService
) : ViewModel() {

    private val _studioState = MutableStateFlow(DesignStudioUiState())
    val studioState: StateFlow<DesignStudioUiState> = _studioState.asStateFlow()

    val savedProjects: StateFlow<List<DesignProjectEntity>> = designRepository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val userProfile: StateFlow<UserProfileState> = userPreferencesRepository.userProfileFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserProfileState()
        )

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Üdvözöllek! Én a **RoomAI Assistant** vagyok, a személyes AI lakberendeződ és belsőépítész tanácsadód.\n\nKérdezz bátran a feltöltött szobádról, színpalettákról, bútorválasztásról, tértágító trükkökről vagy költséghatékony felújítási ötletekről!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAssistantThinking = MutableStateFlow(false)
    val isAssistantThinking: StateFlow<Boolean> = _isAssistantThinking.asStateFlow()

    init {
        viewModelScope.launch {
            designRepository.ensureSeedSampleProjectsLoaded()
        }
    }

    fun clearSnackbarMessage() {
        _studioState.update { it.copy(snackbarMessage = null) }
    }

    fun showSnackbar(message: String) {
        _studioState.update { it.copy(snackbarMessage = message) }
    }

    /**
     * Handles photo selection from Android Photo Picker or Camera capture.
     * Validates format, compresses image, and automatically starts Gemini AI Room Analysis.
     */
    fun onPhotoSelected(uri: Uri) {
        viewModelScope.launch {
            _studioState.update {
                it.copy(
                    isCompressingPhoto = true,
                    uploadError = null
                )
            }
            val result = ImageUtils.loadAndCompressUri(appContext, uri)
            result.fold(
                onSuccess = { bitmap ->
                    _studioState.update {
                        it.copy(
                            uploadedBitmap = bitmap,
                            isUploadedDemoPhoto = false,
                            isCompressingPhoto = false,
                            uploadError = null,
                            analysisState = AnalysisUiState.Idle,
                            generationState = GenerationUiState.Idle
                        )
                    }
                    analyzeCurrentRoom()
                },
                onFailure = { err ->
                    _studioState.update {
                        it.copy(
                            isCompressingPhoto = false,
                            uploadError = err.localizedMessage ?: "Érvénytelen képfájl."
                        )
                    }
                }
            )
        }
    }

    /**
     * Loads the bundled sample before-room photograph so the user can test either
     * real Gemini AI generation on the sample room or explore the interactive demo mode.
     */
    fun loadSampleRoomPhotograph(runRealAiAnalysis: Boolean = false) {
        viewModelScope.launch {
            _studioState.update { it.copy(isCompressingPhoto = true, uploadError = null) }
            val sampleBefore = ImageUtils.loadDrawableBitmap(
                appContext,
                R.drawable.img_hero_living_before_1793839689397
            )
            _studioState.update {
                it.copy(
                    uploadedBitmap = sampleBefore,
                    isUploadedDemoPhoto = true,
                    isCompressingPhoto = false,
                    uploadError = null,
                    analysisState = if (runRealAiAnalysis) AnalysisUiState.Idle else AnalysisUiState.Success(
                        RoomCatalog.sampleDemoAnalysis
                    )
                )
            }
            if (runRealAiAnalysis) {
                analyzeCurrentRoom()
            }
        }
    }

    fun removeUploadedPhoto() {
        _studioState.update {
            it.copy(
                uploadedBitmap = null,
                isUploadedDemoPhoto = false,
                uploadError = null,
                analysisState = AnalysisUiState.Idle,
                generationState = GenerationUiState.Idle
            )
        }
    }

    /**
     * Runs real Gemini AI Room Analysis on the currently uploaded photograph.
     */
    fun analyzeCurrentRoom() {
        val currentBitmap = _studioState.value.uploadedBitmap
        if (currentBitmap == null) {
            _studioState.update {
                it.copy(uploadError = "Kérjük, először tölts fel egy fényképet a szobádról!")
            }
            return
        }

        viewModelScope.launch {
            _studioState.update { it.copy(analysisState = AnalysisUiState.Loading) }
            val result = aiService.analyzeRoomPhotograph(currentBitmap)
            result.fold(
                onSuccess = { analysis ->
                    _studioState.update {
                        it.copy(analysisState = AnalysisUiState.Success(analysis))
                    }
                },
                onFailure = { error ->
                    _studioState.update {
                        it.copy(
                            analysisState = AnalysisUiState.Error(
                                error.localizedMessage ?: "Nem sikerült elemezni a szobát."
                            )
                        )
                    }
                }
            )
        }
    }

    fun selectStyle(style: InteriorStyle) {
        _studioState.update { it.copy(selectedStyle = style) }
    }

    fun selectColorPalette(palette: ColorPaletteOption) {
        _studioState.update { it.copy(selectedColorPalette = palette) }
    }

    fun selectBudget(budget: BudgetTier) {
        _studioState.update { it.copy(selectedBudget = budget) }
    }

    fun selectAtmosphere(atmosphere: AtmospherePreference) {
        _studioState.update { it.copy(selectedAtmosphere = atmosphere) }
    }

    fun selectDecorDensity(density: DecorDensity) {
        _studioState.update { it.copy(selectedDecorDensity = density) }
    }

    fun toggleFurniturePreference(pref: String) {
        _studioState.update { state ->
            val updated = state.selectedFurniturePreferences.toMutableSet()
            if (updated.contains(pref)) updated.remove(pref) else updated.add(pref)
            state.copy(selectedFurniturePreferences = updated)
        }
    }

    fun updateCustomInstructions(instructions: String) {
        _studioState.update { it.copy(customInstructions = instructions) }
    }

    fun appendQuickInstruction(snippet: String) {
        _studioState.update { state ->
            val current = state.customInstructions.trim()
            val combined = if (current.isEmpty()) snippet else "$current $snippet"
            state.copy(customInstructions = combined)
        }
    }

    /**
     * Generates a redesigned room image using Google's Gemini image generation model
     * (`gemini-nano-banana-2.1`). Never substitutes a failed API call with fake output.
     */
    fun generateRoomRedesign(onNavigateToComparison: () -> Unit) {
        val state = _studioState.value
        val originalBitmap = state.uploadedBitmap
        if (originalBitmap == null) {
            _studioState.update {
                it.copy(
                    uploadError = "Hiányzó szobafotó! Kérjük, tölts fel egy képet az átalakítás előtt."
                )
            }
            return
        }

        val profile = userProfile.value
        if (!profile.canGenerateAiImage) {
            _studioState.update {
                it.copy(
                    generationState = GenerationUiState.Error(
                        "Elérted az ingyenes csomag 2 AI képgenerálási limitjét. Válts RoomAI Prémiumra a korlátlan tervezéshez, vagy próbáld ki a Bemutató (Demo) módot!"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _studioState.update {
                it.copy(
                    generationState = GenerationUiState.Loading(
                        "A Gemini Nano Banana AI modell újratervezi a szobádat (${state.selectedStyle.nameHu} stílus)... Ez 30–60 másodpercet vehet igénybe."
                    )
                )
            }

            // Ensure room analysis exists or run it if idle
            val analysisResult = (state.analysisState as? AnalysisUiState.Success)?.analysis

            val result = aiService.generateRedesignedRoomImage(
                originalRoomBitmap = originalBitmap,
                style = state.selectedStyle,
                colorPalette = state.selectedColorPalette,
                budget = state.selectedBudget,
                atmosphere = state.selectedAtmosphere,
                decorDensity = state.selectedDecorDensity,
                selectedFurniturePrefs = state.selectedFurniturePreferences,
                customInstructions = state.customInstructions,
                roomAnalysis = analysisResult
            )

            result.fold(
                onSuccess = { generatedBitmap ->
                    userPreferencesRepository.incrementGenerationCount()
                    val roomTypeTitle = analysisResult?.roomType ?: "Szobaterv"
                    val savedEntity = designRepository.saveGeneratedProject(
                        title = "${state.selectedStyle.nameHu} $roomTypeTitle",
                        roomType = roomTypeTitle,
                        styleId = state.selectedStyle.id,
                        styleNameHu = state.selectedStyle.nameHu,
                        colorPaletteNameHu = state.selectedColorPalette.nameHu,
                        budgetLabelHu = state.selectedBudget.labelHu,
                        customPrompt = state.customInstructions,
                        originalBitmap = originalBitmap,
                        generatedBitmap = generatedBitmap,
                        analysisSummary = analysisResult?.designRecommendation
                            ?: "${state.selectedStyle.nameHu} stílusú AI látványterv.",
                        estimatedDimensions = analysisResult?.estimatedDimensions
                            ?: "Vizuális becslés a fotó alapján",
                        isDemoSample = false
                    )

                    _studioState.update {
                        it.copy(
                            generationState = GenerationUiState.Success(
                                originalBitmap = originalBitmap,
                                generatedBitmap = generatedBitmap,
                                isDemoSample = false,
                                savedProjectId = savedEntity.id
                            ),
                            activeComparisonOriginal = originalBitmap,
                            activeComparisonGenerated = generatedBitmap,
                            activeComparisonTitle = savedEntity.title,
                            activeComparisonStyleName = state.selectedStyle.nameHu,
                            activeComparisonIsDemo = false
                        )
                    }
                    onNavigateToComparison()
                },
                onFailure = { err ->
                    _studioState.update {
                        it.copy(
                            generationState = GenerationUiState.Error(
                                err.localizedMessage ?: "A képgenerálás sikertelen volt."
                            )
                        )
                    }
                }
            )
        }
    }

    /**
     * Explicit Prototype Demo workflow: clearly labeled as Sample/Demo result so users can
     * test the entire Before/After Comparison, Gallery, Save and Share flow immediately.
     */
    fun launchInteractiveDemoShowcase(onNavigateToComparison: () -> Unit) {
        viewModelScope.launch {
            val beforeBmp = ImageUtils.loadDrawableBitmap(
                appContext,
                R.drawable.img_hero_living_before_1793839689397
            )
            val style = _studioState.value.selectedStyle
            val afterBmp = ImageUtils.loadDrawableBitmap(
                appContext,
                style.previewResId
            )

            _studioState.update {
                it.copy(
                    uploadedBitmap = beforeBmp,
                    isUploadedDemoPhoto = true,
                    analysisState = AnalysisUiState.Success(RoomCatalog.sampleDemoAnalysis),
                    generationState = GenerationUiState.Success(
                        originalBitmap = beforeBmp,
                        generatedBitmap = afterBmp,
                        isDemoSample = true
                    ),
                    activeComparisonOriginal = beforeBmp,
                    activeComparisonGenerated = afterBmp,
                    activeComparisonTitle = "${style.nameHu} Nappali (Minta Demó)",
                    activeComparisonStyleName = style.nameHu,
                    activeComparisonIsDemo = true
                )
            }
            onNavigateToComparison()
        }
    }

    /**
     * Opens any saved project from My Designs / Gallery into the Before/After Comparison screen.
     */
    fun openSavedProjectInComparison(
        project: DesignProjectEntity,
        onReadyToNavigate: () -> Unit
    ) {
        viewModelScope.launch {
            val origBmp = ImageUtils.loadBitmapFromPathOrRes(appContext, project.originalImagePath)
                ?: ImageUtils.loadDrawableBitmap(appContext, R.drawable.img_hero_living_before_1793839689397)
            val genBmp = ImageUtils.loadBitmapFromPathOrRes(appContext, project.generatedImagePath)
                ?: ImageUtils.loadDrawableBitmap(appContext, R.drawable.img_hero_living_after_1793839687876)

            _studioState.update {
                it.copy(
                    activeComparisonOriginal = origBmp,
                    activeComparisonGenerated = genBmp,
                    activeComparisonTitle = project.title,
                    activeComparisonStyleName = project.styleNameHu,
                    activeComparisonIsDemo = project.isDemoSample
                )
            }
            onReadyToNavigate()
        }
    }

    fun saveCurrentComparisonToDevice() {
        val genBitmap = _studioState.value.activeComparisonGenerated ?: return
        val styleName = _studioState.value.activeComparisonStyleName
        val isHighRes = userProfile.value.isPremium

        viewModelScope.launch {
            val res = ImageUtils.saveBitmapToGallery(
                context = appContext,
                bitmap = genBitmap,
                title = styleName,
                highResolution = isHighRes
            )
            res.fold(
                onSuccess = { msg -> showSnackbar(msg) },
                onFailure = { err -> showSnackbar("Mentési hiba: ${err.localizedMessage}") }
            )
        }
    }

    fun shareCurrentComparisonImage() {
        val genBitmap = _studioState.value.activeComparisonGenerated ?: return
        val styleName = _studioState.value.activeComparisonStyleName
        viewModelScope.launch {
            val res = ImageUtils.shareBitmap(appContext, genBitmap, styleName)
            res.onFailure { err ->
                showSnackbar("Megosztási hiba: ${err.localizedMessage}")
            }
        }
    }

    fun deleteSavedProject(projectId: Long) {
        viewModelScope.launch {
            designRepository.deleteProject(projectId)
            showSnackbar("A terv törölve lett a saját galériából.")
        }
    }

    /**
     * Sends a message in the RoomAI Assistant screen and streams the response from Gemini 3.8 Flash.
     */
    fun sendAssistantMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isAssistantThinking.value) return

        val userMsg = ChatMessage(isUser = true, text = trimmed)
        val placeholderAiMsg = ChatMessage(isUser = false, text = "", isStreaming = true)

        _chatMessages.update { it + userMsg + placeholderAiMsg }
        _isAssistantThinking.value = true

        val state = _studioState.value
        val currentAnalysis = (state.analysisState as? AnalysisUiState.Success)?.analysis

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    aiService.streamDesignAssistantReply(
                        userQuestion = trimmed,
                        currentStyle = state.selectedStyle,
                        currentColorPalette = state.selectedColorPalette,
                        currentBudget = state.selectedBudget,
                        currentAnalysis = currentAnalysis,
                        roomBitmap = state.uploadedBitmap
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        val currentText = accumulated.toString()
                        _chatMessages.update { list ->
                            list.map { msg ->
                                if (msg.id == placeholderAiMsg.id) {
                                    msg.copy(text = currentText, isStreaming = true)
                                } else msg
                            }
                        }
                    }
                }
                val finalReply = accumulated.toString().ifBlank {
                    "Sajnálom, a modell nem küldött szöveges választ. Kérjük, próbáld újra!"
                }
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == placeholderAiMsg.id) {
                            msg.copy(text = finalReply, isStreaming = false)
                        } else msg
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Assistant stream timed out", e)
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == placeholderAiMsg.id) {
                            msg.copy(
                                text = "A válaszadás időtúllépés miatt megszakadt (60 mp). Kérjük, próbáld újra!",
                                isStreaming = false
                            )
                        } else msg
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Assistant stream failed", e)
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == placeholderAiMsg.id) {
                            msg.copy(
                                text = "Hiba történt a Gemini AI válasz generálásakor: ${e.localizedMessage ?: "Ismeretlen hiba"}",
                                isStreaming = false
                            )
                        } else msg
                    }
                }
            } finally {
                _isAssistantThinking.value = false
            }
        }
    }

    /**
     * Simulated subscription plan selection (Clearly labeled as prototype simulation, no real money charged).
     */
    fun selectSubscriptionPlan(plan: SubscriptionPlan) {
        viewModelScope.launch {
            userPreferencesRepository.setSubscriptionPlan(plan)
            showSnackbar("Szimulált előfizetési csomag frissítve: ${plan.titleHu} (Prototípus mód – nem történt valódi terhelés).")
        }
    }

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguage(language)
            showSnackbar("Nyelvi beállítás mentve: ${language.label}")
        }
    }

    fun updateUserProfile(name: String, email: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateProfile(name, email)
            showSnackbar("Profil adatok frissítve.")
        }
    }

    fun deleteAccountAndLocalData() {
        viewModelScope.launch {
            userPreferencesRepository.resetAccountData()
            designRepository.clearAllProjects()
            designRepository.ensureSeedSampleProjectsLoaded()
            removeUploadedPhoto()
            showSnackbar("Fiókadatok és saját tervek sikeresen törölve.")
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val db = RoomAiDatabase.getInstance(appContext)
                val designRepo = DesignRepository(appContext, db.designProjectDao())
                val prefsRepo = UserPreferencesRepository(appContext)
                val aiService = GeminiRoomAiService()
                RoomAiViewModel(
                    appContext = appContext,
                    designRepository = designRepo,
                    userPreferencesRepository = prefsRepo,
                    aiService = aiService
                )
            }
        }
    }
}
