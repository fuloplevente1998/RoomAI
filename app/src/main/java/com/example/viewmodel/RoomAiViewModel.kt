package com.example.viewmodel

import android.app.Activity
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
import com.example.data.AiGenerationLogEntity
import com.example.data.BillingTransactionEntity
import com.example.data.DesignProjectEntity
import com.example.data.DesignRepository
import com.example.data.GeminiRoomAiService
import com.example.data.PlayBillingConnectionState
import com.example.data.PlayBillingManager
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
import com.example.model.ThemePreference
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
    private val playBillingManager: PlayBillingManager,
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

    val billingConnectionState: StateFlow<PlayBillingConnectionState> =
        playBillingManager.connectionState

    val billingTransactions: StateFlow<List<BillingTransactionEntity>> =
        designRepository.billingTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val aiGenerationLogs: StateFlow<List<AiGenerationLogEntity>> =
        designRepository.recentAiLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAssistantThinking = MutableStateFlow(false)
    val isAssistantThinking: StateFlow<Boolean> = _isAssistantThinking.asStateFlow()

    init {
        viewModelScope.launch {
            designRepository.ensureSeedDataLoaded()
            designRepository.persistedChatMessages.collect { sqlMessages ->
                if (!_isAssistantThinking.value) {
                    _chatMessages.value = sqlMessages
                }
            }
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
     * Runs real Gemini AI Room Analysis on the currently uploaded photograph and logs SQL telemetry.
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
            val startTime = System.currentTimeMillis()
            _studioState.update { it.copy(analysisState = AnalysisUiState.Loading) }
            val result = aiService.analyzeRoomPhotograph(currentBitmap)
            val elapsed = System.currentTimeMillis() - startTime
            result.fold(
                onSuccess = { analysis ->
                    designRepository.logAiOperation(
                        operationType = "ROOM_ANALYSIS",
                        modelName = "gemini-3.8-flash",
                        styleNameHu = _studioState.value.selectedStyle.nameHu,
                        durationMs = elapsed,
                        isSuccess = true,
                        statusNote = "Elemzett helyiség: ${analysis.roomType}"
                    )
                    _studioState.update {
                        it.copy(analysisState = AnalysisUiState.Success(analysis))
                    }
                },
                onFailure = { error ->
                    designRepository.logAiOperation(
                        operationType = "ROOM_ANALYSIS",
                        modelName = "gemini-3.8-flash",
                        styleNameHu = _studioState.value.selectedStyle.nameHu,
                        durationMs = elapsed,
                        isSuccess = false,
                        statusNote = error.localizedMessage ?: "Elemzési hiba"
                    )
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
     * (`gemini-nano-banana-2.1`) and saves the project + telemetry into Room SQL.
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
                        "Elérted az ingyenes csomag 2 AI képgenerálási limitjét. Válts RoomAI Prémiumra a Google Play Billing felületen, vagy próbáld ki a Bemutató (Demo) módot!"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _studioState.update {
                it.copy(
                    generationState = GenerationUiState.Loading(
                        "A Gemini Nano Banana AI modell újratervezi a szobádat (${state.selectedStyle.nameHu} stílus)... Ez 30–60 másodpercet vehet igénybe."
                    )
                )
            }

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
            val elapsed = System.currentTimeMillis() - startTime

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

                    designRepository.logAiOperation(
                        operationType = "IMAGE_GENERATION",
                        modelName = "gemini-nano-banana-2.1",
                        styleNameHu = state.selectedStyle.nameHu,
                        durationMs = elapsed,
                        isSuccess = true,
                        statusNote = "Mentve SQL-be (Projekt ID: #${savedEntity.id})"
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
                    designRepository.logAiOperation(
                        operationType = "IMAGE_GENERATION",
                        modelName = "gemini-nano-banana-2.1",
                        styleNameHu = state.selectedStyle.nameHu,
                        durationMs = elapsed,
                        isSuccess = false,
                        statusNote = err.localizedMessage ?: "Képgenerálási hiba"
                    )
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
            showSnackbar("A terv törölve lett a Room SQLite adatbázisból.")
        }
    }

    /**
     * Sends a message in RoomAI Assistant, saves both user and streaming AI messages into SQL.
     */
    fun sendAssistantMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isAssistantThinking.value) return

        val state = _studioState.value
        val userMsg = ChatMessage(isUser = true, text = trimmed)
        val placeholderAiMsg = ChatMessage(isUser = false, text = "", isStreaming = true)

        _chatMessages.update { it + userMsg + placeholderAiMsg }
        _isAssistantThinking.value = true

        val currentAnalysis = (state.analysisState as? AnalysisUiState.Success)?.analysis

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            designRepository.insertChatMessage(userMsg, state.selectedStyle.nameHu)
            designRepository.insertChatMessage(placeholderAiMsg, state.selectedStyle.nameHu)

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
                designRepository.updateChatMessageText(placeholderAiMsg.id, finalReply)
                designRepository.logAiOperation(
                    operationType = "ASSISTANT_CHAT",
                    modelName = "gemini-3.8-flash",
                    styleNameHu = state.selectedStyle.nameHu,
                    durationMs = System.currentTimeMillis() - startTime,
                    isSuccess = true,
                    statusNote = "Asszisztens válasz elmentve SQL-be"
                )
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == placeholderAiMsg.id) {
                            msg.copy(text = finalReply, isStreaming = false)
                        } else msg
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Assistant stream timed out", e)
                val timeoutText = "A válaszadás időtúllépés miatt megszakadt (60 mp). Kérjük, próbáld újra!"
                designRepository.updateChatMessageText(placeholderAiMsg.id, timeoutText)
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == placeholderAiMsg.id) {
                            msg.copy(text = timeoutText, isStreaming = false)
                        } else msg
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Assistant stream failed", e)
                val errText = "Hiba történt a Gemini AI válasz generálásakor: ${e.localizedMessage ?: "Ismeretlen hiba"}"
                designRepository.updateChatMessageText(placeholderAiMsg.id, errText)
                _chatMessages.update { list ->
                    list.map { msg ->
                        if (msg.id == placeholderAiMsg.id) {
                            msg.copy(text = errText, isStreaming = false)
                        } else msg
                    }
                }
            } finally {
                _isAssistantThinking.value = false
            }
        }
    }

    fun clearAssistantHistory() {
        viewModelScope.launch {
            designRepository.clearChatHistory()
            showSnackbar("Asszisztens csevegési előzmények törölve az SQL adatbázisból.")
        }
    }

    /**
     * Initiates native Google Play Billing flow if Play Store SKU is available,
     * otherwise returns false so the UI opens the interactive Google Play Test Purchase Sheet.
     */
    fun tryStartNativePlayBilling(activity: Activity, plan: SubscriptionPlan): Boolean {
        return playBillingManager.tryLaunchNativeBillingFlow(activity, plan)
    }

    /**
     * Executes an interactive Google Play Sandbox purchase (Test Card / GPay / Declined Card)
     * and writes the receipt to Room SQL (`billing_transactions`).
     */
    fun completePlaySandboxPurchase(
        plan: SubscriptionPlan,
        paymentMethodLabel: String,
        simulateDecline: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = playBillingManager.executeSandboxPlayPurchase(
                plan = plan,
                paymentMethodLabel = paymentMethodLabel,
                simulateDecline = simulateDecline
            )
            result.fold(
                onSuccess = { tx ->
                    showSnackbar("Google Play Sikeres Vásárlás! Rendelésszám: ${tx.orderId} (${tx.planTitleHu})")
                    onSuccess()
                },
                onFailure = { err ->
                    showSnackbar(err.localizedMessage ?: "Sikertelen fizetési tranzakció.")
                }
            )
        }
    }

    fun refundBillingTransaction(transaction: BillingTransactionEntity) {
        viewModelScope.launch {
            playBillingManager.refundOrCancelTransaction(transaction)
            showSnackbar("Tranzakció visszatérítve és előfizetés lemondva (${transaction.orderId}).")
        }
    }

    fun refreshPlayBillingStatus() {
        playBillingManager.startPlayBillingConnection()
        showSnackbar("Google Play Billing kapcsolat és vásárlások szinkronizálva.")
    }

    fun selectLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPreferencesRepository.setLanguage(language)
            showSnackbar("Nyelvi beállítás mentve: ${language.label}")
        }
    }

    fun selectThemePreference(themePreference: ThemePreference) {
        viewModelScope.launch {
            userPreferencesRepository.setThemePreference(themePreference)
            showSnackbar("Megjelenési téma: ${themePreference.labelHu}")
        }
    }

    fun toggleLightDarkTheme() {
        val current = userProfile.value.themePreference
        val next = if (current == ThemePreference.DARK) {
            ThemePreference.LIGHT
        } else {
            ThemePreference.DARK
        }
        selectThemePreference(next)
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
            designRepository.clearAllSqlTables()
            designRepository.ensureSeedDataLoaded()
            removeUploadedPhoto()
            showSnackbar("Minden SQL tábla és fiókadat sikeresen alaphelyzetbe állítva.")
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val db = RoomAiDatabase.getInstance(appContext)
                val prefsRepo = UserPreferencesRepository(appContext)
                val designRepo = DesignRepository(
                    context = appContext,
                    projectDao = db.designProjectDao(),
                    chatHistoryDao = db.chatHistoryDao(),
                    aiLogDao = db.aiGenerationLogDao(),
                    billingDao = db.billingTransactionDao()
                )
                val billingManager = PlayBillingManager(
                    context = appContext,
                    billingTransactionDao = db.billingTransactionDao(),
                    userPreferencesRepository = prefsRepo
                )
                val aiService = GeminiRoomAiService(appContext)
                RoomAiViewModel(
                    appContext = appContext,
                    designRepository = designRepo,
                    userPreferencesRepository = prefsRepo,
                    playBillingManager = billingManager,
                    aiService = aiService
                )
            }
        }
    }
}
