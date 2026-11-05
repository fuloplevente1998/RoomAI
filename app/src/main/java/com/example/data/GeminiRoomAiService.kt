package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.model.AtmospherePreference
import com.example.model.BudgetTier
import com.example.model.ColorPaletteOption
import com.example.model.DecorDensity
import com.example.model.InteriorStyle
import com.example.model.RoomAnalysis
import com.example.util.ensureFirebaseInitialized
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiRoomAiService"

class GeminiRoomAiService(
    private val context: Context
) {

    // 1. Structured JSON schema for Room Analysis
    private val roomAnalysisSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "roomType" to Schema.string(description = "A szoba típusa magyarul (pl. Nappali, Hálószoba, Konyha, Étkező, Fürdőszoba, Mellékhelyiség, Dolgozószoba)"),
                "estimatedDimensions" to Schema.string(description = "Becsült szobaméretek magyarul, egyértelműen jelezve, hogy vizuális becslés (pl. Kb. 4.0 m x 4.8 m, ~19 m² - becsült érték)"),
                "existingFurniture" to Schema.array(
                    items = Schema.string(),
                    description = "A képen látható meglévő bútorok, szaniterek és főbb tárgyak felsorolása magyarul"
                ),
                "wallColors" to Schema.string(description = "A falak színe, burkolata és állapota magyarul"),
                "flooring" to Schema.string(description = "A padlóburkolat típusa és színe magyarul"),
                "windowsAndDoors" to Schema.string(description = "Az ablakok és ajtók elhelyezkedése és jellege magyarul"),
                "lightingConditions" to Schema.string(description = "A természetes és mesterséges fényviszonyok leírása magyarul"),
                "existingLayout" to Schema.string(description = "A jelenlegi térszervezés és elrendezés leírása magyarul"),
                "designRecommendation" to Schema.string(description = "Rövid, szakértői belsőépítészeti javaslat magyarul a helyiség előnyös átalakítására és felújítására")
            )
        )
    }

    // 2. Lazy initialization of Gemini 3.8 Flash for structured room analysis
    private val analysisModel by lazy {
        ensureFirebaseInitialized(context)
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = roomAnalysisSchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    // 3. Lazy initialization of Gemini Nano Banana 2.1 for photorealistic room redesign image generation
    private val imageGenerationModel by lazy {
        ensureFirebaseInitialized(context)
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    // 4. Lazy initialization of Gemini 3.8 Flash for RoomAI Assistant chat
    private val assistantModel by lazy {
        ensureFirebaseInitialized(context)
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    /**
     * Analyzes an uploaded room photograph and returns a structured RoomAnalysis in Hungarian.
     */
    suspend fun analyzeRoomPhotograph(roomBitmap: Bitmap): Result<RoomAnalysis> {
        return try {
            ensureFirebaseInitialized(context)
            val response = withTimeout(60_000L) {
                analysisModel.generateContent(
                    content {
                        image(roomBitmap)
                        text(
                            """
                            Elemezd ezt a belsőépítészeti szobafotót részletesen magyar nyelven!
                            Határozd meg a helyiség típusát, a vizuálisan becsülhető méreteket (mindig hangsúlyozd, hogy fotó alapján készült becslés, nem pontos mérés!), a meglévő bútorokat vagy szanitereket, a falak színét/állapotát, a padlóburkolatot, az ablakokat és ajtókat, a világítási viszonyokat és a jelenlegi elrendezést.
                            """.trimIndent()
                        )
                    }
                )
            }

            val jsonString = response.text
                ?: return Result.failure(IllegalStateException("A Gemini modell üres választ adott a szobaelemzés során."))

            val json = JSONObject(jsonString)
            val furnitureJsonArray = json.optJSONArray("existingFurniture")
            val furnitureList = mutableListOf<String>()
            if (furnitureJsonArray != null) {
                for (i in 0 until furnitureJsonArray.length()) {
                    furnitureList.add(furnitureJsonArray.getString(i))
                }
            }

            val analysis = RoomAnalysis(
                roomType = json.optString("roomType", "Helyiség"),
                estimatedDimensions = json.optString(
                    "estimatedDimensions",
                    "Kb. 2.5 m × 3.5 m (vizuális becslés a fotó alapján)"
                ),
                existingFurniture = if (furnitureList.isEmpty()) listOf("Alapvető berendezési tárgyak") else furnitureList,
                wallColors = json.optString("wallColors", "Világos semleges árnyalat"),
                flooring = json.optString("flooring", "Meleg tónusú padlóburkolat"),
                windowsAndDoors = json.optString("windowsAndDoors", "Természetes fényt biztosító nyílászárók"),
                lightingConditions = json.optString("lightingConditions", "Természetes nappali megvilágítás"),
                existingLayout = json.optString("existingLayout", "Hagyományos helyiség-elrendezés"),
                designRecommendation = json.optString(
                    "designRecommendation",
                    "A tér kiváló alapot nyújt egy harmonikus, modern lakberendezési átalakításhoz."
                ),
                isDemoSample = false
            )
            Result.success(analysis)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Room analysis timed out", e)
            Result.failure(Exception("A szobaelemzés időtúllépés miatt megszakadt (60 mp). Kérjük, próbáld újra!"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Room analysis failed", e)
            Result.failure(
                Exception("Hiba a Gemini szobaelemzés során: ${e.localizedMessage ?: "Ismeretlen API hiba."}")
            )
        }
    }

    /**
     * Generates a photorealistic redesigned room image from the uploaded room photograph
     * and the user's selected style & parameters using gemini-nano-banana-2.1.
     */
    suspend fun generateRedesignedRoomImage(
        originalRoomBitmap: Bitmap,
        style: InteriorStyle,
        colorPalette: ColorPaletteOption,
        budget: BudgetTier,
        atmosphere: AtmospherePreference,
        decorDensity: DecorDensity,
        selectedFurniturePrefs: Set<String>,
        customInstructions: String,
        roomAnalysis: RoomAnalysis?
    ): Result<Bitmap> {
        return try {
            ensureFirebaseInitialized(context)
            val furnitureContext = if (selectedFurniturePrefs.isNotEmpty()) {
                "Incorporate these requested elements naturally where appropriate for the room type: ${selectedFurniturePrefs.joinToString(", ")}."
            } else {
                ""
            }
            val customContext = if (customInstructions.isNotBlank()) {
                "Additional specific user instructions to follow carefully: $customInstructions"
            } else {
                ""
            }
            val roomTypeHint = roomAnalysis?.roomType ?: "interior room"

            val detailedEditPrompt = """
                Transform this real photograph of a $roomTypeHint into a breathtaking, magazine-quality photorealistic renovated interior design in the ${style.nameEn} (${style.nameHu}) style.
                
                STRICT ARCHITECTURAL PRESERVATION RULES:
                - Preserve the exact original room architecture, walls, ceiling height, and room geometry.
                - Preserve the exact camera angle, perspective, and lens focal length.
                - Preserve the exact positions and proportions of all windows, doors, and structural openings.
                - Maintain realistic human-scale proportions and keep the functional purpose of the space intact.
                
                DESIGN & STYLING INSTRUCTIONS:
                - Style direction: ${style.promptInstruction}.
                - Color palette: ${colorPalette.promptDescription}.
                - Budget & material tier: ${budget.promptHint}.
                - Lighting & atmosphere: ${atmosphere.promptHint}.
                - Decoration density: ${decorDensity.promptHint}.
                $furnitureContext
                $customContext
                
                OUTPUT QUALITY:
                - Produce a 100% photorealistic architectural interior photograph with natural global illumination, realistic contact shadows, and authentic material textures (tile, stone, wood grain, fabric weave, glass reflections).
                - Strictly avoid cartoon, illustration, sketch, or CGI-looking artifacts.
            """.trimIndent()

            val response = withTimeout(60_000L) {
                imageGenerationModel.generateContent(
                    content {
                        image(originalRoomBitmap)
                        text(detailedEditPrompt)
                    }
                )
            }

            val generatedBitmap = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?.image

            if (generatedBitmap != null) {
                Result.success(generatedBitmap)
            } else {
                Result.failure(
                    IllegalStateException("A képgeneráló modell nem küldött vissza képet. Kérjük, próbáld újra vagy módosítsd az utasítást.")
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(
                Exception("A képgenerálás időtúllépés miatt megszakadt (60 mp). Kérjük, próbáld újra!")
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(
                Exception("AI képgenerálási hiba: ${e.localizedMessage ?: "A szolgáltatás jelenleg nem érhető el."}")
            )
        }
    }

    /**
     * Streams helpful Hungarian interior design advice from RoomAI Assistant,
     * aware of the user's current room analysis, style, and budget preferences.
     */
    fun streamDesignAssistantReply(
        userQuestion: String,
        currentStyle: InteriorStyle,
        currentColorPalette: ColorPaletteOption,
        currentBudget: BudgetTier,
        currentAnalysis: RoomAnalysis?,
        roomBitmap: Bitmap?
    ): Flow<String> {
        ensureFirebaseInitialized(context)
        val contextPrompt = buildString {
            appendLine("Te a 'RoomAI Assistant' vagy, egy prémium, barátságos és magasan képzett belsőépítész és lakberendező szakértő.")
            appendLine("Mindig magyar nyelven, választékosan, gyakorlatiasan és jól áttekinthető pontokba szedve válaszolj!")
            appendLine("Jelenlegi tervezési kontextus:")
            appendLine("- Kiválasztott stílus: ${currentStyle.nameHu} (${currentStyle.shortDescriptionHu})")
            appendLine("- Színpaletta: ${currentColorPalette.nameHu}")
            appendLine("- Költségkeret: ${currentBudget.labelHu}")
            if (currentAnalysis != null) {
                appendLine("- Elemzett helyiség típusa: ${currentAnalysis.roomType}")
                appendLine("- Becsült méret: ${currentAnalysis.estimatedDimensions}")
                appendLine("- Meglévő bútorok/szaniterek: ${currentAnalysis.existingFurniture.joinToString(", ")}")
                appendLine("- Falak és padló: ${currentAnalysis.wallColors}, ${currentAnalysis.flooring}")
                appendLine("- Fényviszonyok: ${currentAnalysis.lightingConditions}")
            }
            appendLine()
            appendLine("A felhasználó kérdése: $userQuestion")
        }

        val inputContent = content {
            if (roomBitmap != null) {
                image(roomBitmap)
            }
            text(contextPrompt)
        }

        return assistantModel.generateContentStream(inputContent).map { chunk ->
            chunk.text.orEmpty()
        }
    }
}
