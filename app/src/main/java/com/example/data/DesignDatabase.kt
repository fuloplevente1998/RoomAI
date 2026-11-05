package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/**
 * 1. Mentett belsőépítészeti projektek (SQL tábla: design_projects)
 */
@Entity(tableName = "design_projects")
data class DesignProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val roomType: String,
    val styleId: String,
    val styleNameHu: String,
    val colorPaletteNameHu: String,
    val budgetLabelHu: String,
    val customPrompt: String,
    val originalImagePath: String,
    val generatedImagePath: String,
    val analysisSummary: String,
    val estimatedDimensions: String,
    val isDemoSample: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 2. Google Play Billing és Teszt Sandbox Tranzakciók (SQL tábla: billing_transactions)
 */
@Entity(tableName = "billing_transactions")
data class BillingTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: String,
    val productId: String,
    val planTitleHu: String,
    val amountFormatted: String,
    val purchaseToken: String,
    val paymentMethodLabel: String,
    val status: String, // "PURCHASED_PLAY_BILLING", "PURCHASED_SANDBOX_TEST", "REFUNDED", "CANCELLED"
    val isAcknowledged: Boolean = true,
    val isAutoRenewing: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 3. RoomAI Assistant Csevegési Előzmények (SQL tábla: assistant_chat_messages)
 */
@Entity(tableName = "assistant_chat_messages")
data class ChatHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageUuid: String,
    val isUser: Boolean,
    val text: String,
    val styleContext: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 4. AI Generálási és Szobaelemzési Napló (SQL tábla: ai_generation_logs)
 */
@Entity(tableName = "ai_generation_logs")
data class AiGenerationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String, // "IMAGE_GENERATION", "ROOM_ANALYSIS", "ASSISTANT_CHAT"
    val modelName: String,     // "gemini-nano-banana-2.1", "gemini-3.8-flash"
    val styleNameHu: String,
    val durationMs: Long,
    val isSuccess: Boolean,
    val statusNote: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DesignProjectDao {
    @Query("SELECT * FROM design_projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<DesignProjectEntity>>

    @Query("SELECT * FROM design_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): DesignProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: DesignProjectEntity): Long

    @Query("DELETE FROM design_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("DELETE FROM design_projects")
    suspend fun deleteAllProjects()

    @Query("SELECT COUNT(*) FROM design_projects")
    suspend fun getProjectCount(): Int
}

@Dao
interface BillingTransactionDao {
    @Query("SELECT * FROM billing_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<BillingTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: BillingTransactionEntity): Long

    @Query("UPDATE billing_transactions SET status = :newStatus, isAutoRenewing = 0 WHERE id = :id")
    suspend fun updateTransactionStatus(id: Long, newStatus: String)

    @Query("DELETE FROM billing_transactions")
    suspend fun clearAllTransactions()
}

@Dao
interface ChatHistoryDao {
    @Query("SELECT * FROM assistant_chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatHistoryEntity>>

    @Query("SELECT COUNT(*) FROM assistant_chat_messages")
    suspend fun getMessageCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatHistoryEntity): Long

    @Query("UPDATE assistant_chat_messages SET text = :updatedText WHERE messageUuid = :uuid")
    suspend fun updateMessageByUuid(uuid: String, updatedText: String)

    @Query("DELETE FROM assistant_chat_messages")
    suspend fun clearChatHistory()
}

@Dao
interface AiGenerationLogDao {
    @Query("SELECT * FROM ai_generation_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<AiGenerationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AiGenerationLogEntity): Long

    @Query("DELETE FROM ai_generation_logs")
    suspend fun clearLogs()
}

@Database(
    entities = [
        DesignProjectEntity::class,
        BillingTransactionEntity::class,
        ChatHistoryEntity::class,
        AiGenerationLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class RoomAiDatabase : RoomDatabase() {
    abstract fun designProjectDao(): DesignProjectDao
    abstract fun billingTransactionDao(): BillingTransactionDao
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun aiGenerationLogDao(): AiGenerationLogDao

    companion object {
        @Volatile
        private var INSTANCE: RoomAiDatabase? = null

        fun getInstance(context: Context): RoomAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoomAiDatabase::class.java,
                    "roomai_projects.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
