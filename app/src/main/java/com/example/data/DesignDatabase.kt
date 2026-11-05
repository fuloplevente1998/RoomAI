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

@Database(
    entities = [DesignProjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RoomAiDatabase : RoomDatabase() {
    abstract fun designProjectDao(): DesignProjectDao

    companion object {
        @Volatile
        private var INSTANCE: RoomAiDatabase? = null

        fun getInstance(context: Context): RoomAiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RoomAiDatabase::class.java,
                    "roomai_projects.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
