package com.example.data

import android.content.Context
import android.graphics.Bitmap
import com.example.R
import com.example.model.RoomCatalog
import com.example.util.ImageUtils
import kotlinx.coroutines.flow.Flow

/**
 * Repository managing saved interior design projects locally via Room,
 * structured so a future CloudStorageService (Firebase Storage + Firestore) can sync seamlessly.
 */
class DesignRepository(
    private val context: Context,
    private val dao: DesignProjectDao
) {
    val allProjects: Flow<List<DesignProjectEntity>> = dao.getAllProjects()

    suspend fun ensureSeedSampleProjectsLoaded() {
        if (dao.getProjectCount() == 0) {
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
            dao.insertProject(sample1)
            dao.insertProject(sample2)
            dao.insertProject(sample3)
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
        val newId = dao.insertProject(entity)
        return entity.copy(id = newId)
    }

    suspend fun deleteProject(id: Long) {
        dao.deleteProjectById(id)
    }

    suspend fun clearAllProjects() {
        dao.deleteAllProjects()
    }
}
