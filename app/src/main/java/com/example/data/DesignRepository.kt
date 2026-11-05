package com.example.data

import android.content.Context
import android.graphics.Bitmap
import com.example.R
import com.example.model.ChatMessage
import com.example.model.RoomCatalog
import com.example.util.ImageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Full SQL-backed Repository coordinating all 4 Room SQLite tables:
 * 1. `design_projects` (Saved Interior Design Projects)
 * 2. `billing_transactions` (Google Play Billing & Sandbox Transactions)
 * 3. `assistant_chat_messages` (RoomAI Assistant Chat History)
 * 4. `ai_generation_logs` (Gemini AI Performance & Telemetry Logs)
 */
class DesignRepository(
    private val context: Context,
    private val projectDao: DesignProjectDao,
    private val chatHistoryDao: ChatHistoryDao,
    private val aiLogDao: AiGenerationLogDao,
    private val billingDao: BillingTransactionDao
) {
    val allProjects: Flow<List<DesignProjectEntity>> = projectDao.getAllProjects()

    val persistedChatMessages: Flow<List<ChatMessage>> = chatHistoryDao.getAllMessages().map { list ->
        list.map { entity ->
            ChatMessage(
                id = entity.messageUuid,
                isUser = entity.isUser,
                text = entity.text,
                timestamp = entity.timestamp,
                isStreaming = false
            )
        }
    }

    val recentAiLogs: Flow<List<AiGenerationLogEntity>> = aiLogDao.getRecentLogs()

    val billingTransactions: Flow<List<BillingTransactionEntity>> = billingDao.getAllTransactions()

    suspend fun ensureSeedDataLoaded() {
        if (projectDao.getProjectCount() == 0) {
            val sample1 = DesignProjectEntity(
                title = "Skandináv-Japandi Nappali",
                roomType = "Nappali (Living Room)",
                styleId = "scandinavian",
                styleNameHu = "Skandináv",
                colorPaletteNameHu = "Meleg Bézs & Pezsgőarany",
                budgetLabelHu = "Prémium költségvetés",
                customPrompt = "Ívelt bouclé kanapé, olajfa a sarokban, meleg tölgy halszálka parketta és sárgaréz függeszték.",
                originalImagePath = "res:${R.drawable.img_hero_living_before_1793839689397}",
                generatedImagePath = "res:${R.drawable.img_hero_living_after_1793839687876}",
                analysisSummary = RoomCatalog.sampleDemoAnalysis.designRecommendation,
                estimatedDimensions = RoomCatalog.sampleDemoAnalysis.estimatedDimensions,
                isDemoSample = true,
                createdAt = System.currentTimeMillis() - 3_600_000L * 5
            )
            val sample2 = DesignProjectEntity(
                title = "Luxus Modern Hálószoba",
                roomType = "Hálószoba (Bedroom)",
                styleId = "luxury",
                styleNameHu = "Luxus",
                colorPaletteNameHu = "Meleg Bézs & Pezsgőarany",
                budgetLabelHu = "Prémium költségvetés",
                customPrompt = "Kárpitozott fejvég, rejtett LED mennyezeti világítás, travertin éjjeliszekrény.",
                originalImagePath = "res:${R.drawable.img_hero_living_before_1793839689397}",
                generatedImagePath = "res:${R.drawable.img_style_luxury_modern_1793839691630}",
                analysisSummary = "A meleg falburkolat és a rétegzett indirekt világítás exkluzív szállodai hangulatot teremt.",
                estimatedDimensions = "Kb. 3.8 m × 4.5 m (~17 m², becsült érték)",
                isDemoSample = true,
                createdAt = System.currentTimeMillis() - 3_600_000L * 24
            )
            val sample3 = DesignProjectEntity(
                title = "Mediterrán Rusztikus Étkező",
                roomType = "Étkező & Nappali",
                styleId = "mediterranean",
                styleNameHu = "Mediterrán",
                colorPaletteNameHu = "Mediterrán Terrakotta & Föld",
                budgetLabelHu = "Közepes költségvetés",
                customPrompt = "Tömörfa étkezőasztal, fonott rattan lámpabúrák, boltíves átjáró és terrakotta kaspók.",
                originalImagePath = "res:${R.drawable.img_hero_living_before_1793839689397}",
                generatedImagePath = "res:${R.drawable.img_style_mediterranean_rustic_1793839690676}",
                analysisSummary = "A meszelt falak és a természetes rattan textúrák meleg, napsütötte déli atmoszférát biztosítanak.",
                estimatedDimensions = "Kb. 4.5 m × 6.0 m (~27 m², becsült érték)",
                isDemoSample = true,
                createdAt = System.currentTimeMillis() - 3_600_000L * 48
            )
            projectDao.insertProject(sample1)
            projectDao.insertProject(sample2)
            projectDao.insertProject(sample3)
        }

        if (chatHistoryDao.getMessageCount() == 0) {
            chatHistoryDao.insertMessage(
                ChatHistoryEntity(
                    messageUuid = "welcome_initial",
                    isUser = false,
                    text = "Üdvözöllek! Én a **RoomAI Assistant** vagyok, a személyes AI lakberendeződ és belsőépítész tanácsadód.\n\nMinden üzenetünk automatikusan mentésre kerül a helyi Room SQLite adatbázisba. Kérdezz bátran a feltöltött szobádról, színpalettákról, bútorválasztásról vagy költséghatékony felújítási ötletekről!",
                    styleContext = "Skandináv"
                )
            )
        }
    }

    suspend fun saveGeneratedProject(
        title: String,
        roomType: String,
        styleId: String,
        styleNameHu: String,
        colorPaletteNameHu: String,
        budgetLabelHu: String,
        customPrompt: String,
        originalBitmap: Bitmap,
        generatedBitmap: Bitmap,
        analysisSummary: String,
        estimatedDimensions: String,
        isDemoSample: Boolean
    ): DesignProjectEntity {
        val origPath = ImageUtils.saveBitmapToInternalStorage(context, originalBitmap, "orig")
        val genPath = ImageUtils.saveBitmapToInternalStorage(context, generatedBitmap, "gen")

        val entity = DesignProjectEntity(
            title = title,
            roomType = roomType,
            styleId = styleId,
            styleNameHu = styleNameHu,
            colorPaletteNameHu = colorPaletteNameHu,
            budgetLabelHu = budgetLabelHu,
            customPrompt = customPrompt,
            originalImagePath = origPath,
            generatedImagePath = genPath,
            analysisSummary = analysisSummary,
            estimatedDimensions = estimatedDimensions,
            isDemoSample = isDemoSample,
            createdAt = System.currentTimeMillis()
        )
        val newId = projectDao.insertProject(entity)
        return entity.copy(id = newId)
    }

    suspend fun insertChatMessage(message: ChatMessage, styleContext: String) {
        chatHistoryDao.insertMessage(
            ChatHistoryEntity(
                messageUuid = message.id,
                isUser = message.isUser,
                text = message.text,
                styleContext = styleContext,
                timestamp = message.timestamp
            )
        )
    }

    suspend fun updateChatMessageText(uuid: String, updatedText: String) {
        chatHistoryDao.updateMessageByUuid(uuid, updatedText)
    }

    suspend fun clearChatHistory() {
        chatHistoryDao.clearChatHistory()
        ensureSeedDataLoaded()
    }

    suspend fun logAiOperation(
        operationType: String,
        modelName: String,
        styleNameHu: String,
        durationMs: Long,
        isSuccess: Boolean,
        statusNote: String
    ) {
        aiLogDao.insertLog(
            AiGenerationLogEntity(
                operationType = operationType,
                modelName = modelName,
                styleNameHu = styleNameHu,
                durationMs = durationMs,
                isSuccess = isSuccess,
                statusNote = statusNote
            )
        )
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
    }

    suspend fun clearAllSqlTables() {
        projectDao.deleteAllProjects()
        chatHistoryDao.clearChatHistory()
        aiLogDao.clearLogs()
        billingDao.clearAllTransactions()
    }
}
