package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R

data class InteriorStyle(
    val id: String,
    val nameHu: String,
    val nameEn: String,
    val shortDescriptionHu: String,
    val promptInstruction: String,
    @DrawableRes val previewResId: Int,
    val isPremiumOnly: Boolean = false,
    val accentHex: Long = 0xFFB88E52
)

enum class BudgetTier(
    val labelHu: String,
    val subtitleHu: String,
    val promptHint: String
) {
    LOW(
        labelHu = "Alacsony költségvetés",
        subtitleHu = "Költséghatékony, praktikus megoldások",
        promptHint = "practical, cost-effective, accessible modular furniture, smart DIY-friendly styling"
    ),
    MEDIUM(
        labelHu = "Közepes költségvetés",
        subtitleHu = "Minőségi anyagok és kiegyensúlyozott dizájn",
        promptHint = "mid-range quality contemporary furniture, natural wood veneers, curated textiles and warm lighting"
    ),
    PREMIUM(
        labelHu = "Prémium költségvetés",
        subtitleHu = "Luxus alapanyagok, egyedi tervezői bútorok",
        promptHint = "bespoke architectural digest luxury furniture, solid oak, natural marble/travertine, designer lighting fixtures, high-end fabrics"
    )
}

enum class AtmospherePreference(
    val labelHu: String,
    val promptHint: String
) {
    BRIGHT(
        labelHu = "Világos és légies",
        promptHint = "bright, sun-drenched, airy atmosphere with soft natural daylight and warm ambient glow"
    ),
    DARK(
        labelHu = "Hangulatos sötét / Intim",
        promptHint = "moody, intimate, sophisticated warm dim lighting with rich shadows and cozy architectural accent lights"
    )
}

enum class DecorDensity(
    val labelHu: String,
    val promptHint: String
) {
    MINIMAL(
        labelHu = "Letisztult / Minimál",
        promptHint = "clean, uncluttered surfaces, minimal curated accessories, calm negative space"
    ),
    DECORATIVE(
        labelHu = "Gazdagon dekorált",
        promptHint = "richly layered styling with indoor plants, framed wall art, textured cushions, books, ceramics and throws"
    )
}

data class ColorPaletteOption(
    val id: String,
    val nameHu: String,
    val colors: List<Color>,
    val promptDescription: String
)

data class RoomAnalysis(
    val roomType: String,
    val estimatedDimensions: String,
    val existingFurniture: List<String>,
    val wallColors: String,
    val flooring: String,
    val windowsAndDoors: String,
    val lightingConditions: String,
    val existingLayout: String,
    val designRecommendation: String,
    val isDemoSample: Boolean = false
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

enum class SubscriptionPlan(
    val id: String,
    val titleHu: String,
    val priceHu: String,
    val periodHu: String,
    val badgeHu: String? = null
) {
    FREE("free", "Ingyenes csomag", "0 Ft", "Alap hozzáférés"),
    MONTHLY("pro_monthly", "Prémium Havi", "1 490 Ft", "/ hó", "Népszerű"),
    ANNUAL("pro_annual", "Prémium Éves", "11 990 Ft", "/ év", "33% Megtakarítás")
}

enum class AppLanguage(val code: String, val label: String, val statusLabel: String) {
    HUNGARIAN("hu", "Magyar (HU)", "Aktív"),
    ENGLISH("en", "English (EN)", "Előkészítve"),
    GERMAN("de", "Deutsch (DE)", "Előkészítve")
}

object RoomCatalog {

    val styles: List<InteriorStyle> = listOf(
        InteriorStyle(
            id = "modern",
            nameHu = "Modern",
            nameEn = "Modern",
            shortDescriptionHu = "Letisztult vonalak, funkcionális terek és időtálló kortárs elegancia.",
            promptInstruction = "Modern interior design with clean architectural lines, sleek low-profile sofa, warm neutral tones, subtle black metal and warm oak accents",
            previewResId = R.drawable.img_style_luxury_modern_1793839691630,
            isPremiumOnly = false
        ),
        InteriorStyle(
            id = "scandinavian",
            nameHu = "Skandináv",
            nameEn = "Scandinavian",
            shortDescriptionHu = "Világos fafelületek, puha textíliák, hygge hangulat és természetes fény.",
            promptInstruction = "Scandinavian Nordic interior design with light oak wood, cozy bouclé and wool textiles, soft cream and sage tones, abundant natural light, hygge warmth",
            previewResId = R.drawable.img_hero_living_after_1793839687876,
            isPremiumOnly = false
        ),
        InteriorStyle(
            id = "minimalist",
            nameHu = "Minimalista",
            nameEn = "Minimalist",
            shortDescriptionHu = "Harmónia, rendezett felületek és nyugodt, monokróm színharmónia.",
            promptInstruction = "Warm Minimalist architectural interior design, clutter-free serene space, monolithic forms, limewash beige walls, concealed storage, sculptural simplicity",
            previewResId = R.drawable.img_hero_living_after_1793839687876,
            isPremiumOnly = false
        ),
        InteriorStyle(
            id = "luxury",
            nameHu = "Luxus",
            nameEn = "Luxury",
            shortDescriptionHu = "Márvány, selyemfényű szövetek, csiszolt arany részletek és exkluzív világítás.",
            promptInstruction = "High-end Luxury interior design with travertine and marble surfaces, brushed champagne gold accents, velvet and bouclé upholstery, bespoke chandelier, architectural wall paneling",
            previewResId = R.drawable.img_style_luxury_modern_1793839691630,
            isPremiumOnly = true
        ),
        InteriorStyle(
            id = "industrial",
            nameHu = "Indusztriális",
            nameEn = "Industrial",
            shortDescriptionHu = "Látszó tégla, fekete acél szerkezetek, konyakszínű bőr és loft karakter.",
            promptInstruction = "Refined Industrial loft interior design with exposed textured brick or concrete accents, cognac leather seating, matte black steel framing, warm Edison and track lighting, reclaimed wood",
            previewResId = R.drawable.img_style_luxury_modern_1793839691630,
            isPremiumOnly = false
        ),
        InteriorStyle(
            id = "bohemian",
            nameHu = "Bohém",
            nameEn = "Bohemian",
            shortDescriptionHu = "Természetes rattan, buja szobanövények, mintás szőnyegek és művészi szabadság.",
            promptInstruction = "Modern Bohemian eclectic interior design with woven rattan furniture, macramé and layered Moroccan rugs, terracotta pots with lush indoor plants, warm earthy tones",
            previewResId = R.drawable.img_style_mediterranean_rustic_1793839690676,
            isPremiumOnly = false
        ),
        InteriorStyle(
            id = "japandi",
            nameHu = "Japandi",
            nameEn = "Japandi",
            shortDescriptionHu = "A japán wabi-sabi esztétika és a skandináv kényelem tökéletes fúziója.",
            promptInstruction = "Japandi interior design blending Japanese wabi-sabi minimalism and Scandinavian warmth, low-slung solid wood furniture, paper lantern lighting, neutral stone and oat palette, indoor olive or bonsai tree",
            previewResId = R.drawable.img_hero_living_after_1793839687876,
            isPremiumOnly = true
        ),
        InteriorStyle(
            id = "classic",
            nameHu = "Klasszikus",
            nameEn = "Classic",
            shortDescriptionHu = "Szimmetria, díszlécek, nemes fabútorok és polgári szalon hangulat.",
            promptInstruction = "Timeless Classic European interior design with crown molding and wainscoting, symmetrical furniture arrangement, tufted upholstery, herringbone parquet floor, brass sconces",
            previewResId = R.drawable.img_style_luxury_modern_1793839691630,
            isPremiumOnly = true
        ),
        InteriorStyle(
            id = "rustic",
            nameHu = "Rusztikus",
            nameEn = "Rustic",
            shortDescriptionHu = "Masszív gerendák, természetes kő, kézműves kerámiák és otthonos melegség.",
            promptInstruction = "Modern Rustic interior design with weathered solid timber, natural stone textures, cozy linen upholstery, handcrafted pottery, warm hearth ambiance",
            previewResId = R.drawable.img_style_mediterranean_rustic_1793839690676,
            isPremiumOnly = false
        ),
        InteriorStyle(
            id = "mediterranean",
            nameHu = "Mediterrán",
            nameEn = "Mediterranean",
            shortDescriptionHu = "Meszelt falak, terrakotta árnyalatok, olajfák és napsütötte déli nyugalom.",
            promptInstruction = "Sunlit Mediterranean villa interior design with textured limewash walls, terracotta tiles and ceramics, woven jute rugs, natural linen sofas, olive branches and warm coastal light",
            previewResId = R.drawable.img_style_mediterranean_rustic_1793839690676,
            isPremiumOnly = true
        ),
        InteriorStyle(
            id = "contemporary",
            nameHu = "Kortárs (Contemporary)",
            nameEn = "Contemporary",
            shortDescriptionHu = "Ívelt organikus formák, strukturált szövetek és galériaszerű fókuszpontok.",
            promptInstruction = "Curated Contemporary interior design with curved organic bouclé sofa, sculptural coffee table, statement abstract artwork, layered indirect LED and pendant lighting",
            previewResId = R.drawable.img_hero_living_after_1793839687876,
            isPremiumOnly = true
        ),
        InteriorStyle(
            id = "art_deco",
            nameHu = "Art Deco",
            nameEn = "Art Deco",
            shortDescriptionHu = "Geometrikus minták, bársony kárpitok, aranyozott részletek és drámai sikkes tér.",
            promptInstruction = "Glamorous Art Deco revival interior design with geometric brass inlays, fluted wood panels, rich velvet seating, opal glass globe lighting, bold symmetry",
            previewResId = R.drawable.img_style_luxury_modern_1793839691630,
            isPremiumOnly = true
        )
    )

    val colorPalettes: List<ColorPaletteOption> = listOf(
        ColorPaletteOption(
            id = "warm_beige_gold",
            nameHu = "Meleg Bézs & Pezsgőarany",
            colors = listOf(Color(0xFFF5EFE6), Color(0xFFDCCBB5), Color(0xFFB88E52), Color(0xFF3E362E)),
            promptDescription = "warm beige, cream, soft sand, and subtle champagne gold accents"
        ),
        ColorPaletteOption(
            id = "nordic_sage_oak",
            nameHu = "Északi Zsálya & Tölgy",
            colors = listOf(Color(0xFFF7F6F2), Color(0xFFC8D1C5), Color(0xFF6B7B65), Color(0xFFA68966)),
            promptDescription = "soft off-white, muted sage green, natural honey oak wood, and linen tones"
        ),
        ColorPaletteOption(
            id = "terracotta_earth",
            nameHu = "Mediterrán Terrakotta & Föld",
            colors = listOf(Color(0xFFF6EEE5), Color(0xFFD59B7E), Color(0xFFA65B3E), Color(0xFF5C4638)),
            promptDescription = "warm limewash cream, sun-baked terracotta, ochre, and walnut wood"
        ),
        ColorPaletteOption(
            id = "monochrome_charcoal",
            nameHu = "Elegáns Monokróm & Antracit",
            colors = listOf(Color(0xFFF2F2F0), Color(0xFFB8B6B2), Color(0xFF4A4845), Color(0xFF1C1B1A)),
            promptDescription = "sophisticated warm alabaster, greige, stone gray, and matte charcoal accents"
        ),
        ColorPaletteOption(
            id = "royal_emerald_brass",
            nameHu = "Smaragd & Sárgaréz",
            colors = listOf(Color(0xFFF6F2EB), Color(0xFF234F42), Color(0xFFC59B5F), Color(0xFF241F1C)),
            promptDescription = "deep emerald green, warm cream walls, brushed brass, and dark walnut"
        )
    )

    val furniturePreferences: List<String> = listOf(
        "Nagy, kényelmes kanapé",
        "Természetes tölgyfa bútorok",
        "Sok élő szobanövény",
        "Hangulatos olvasósarok",
        "Modern étkezőasztal",
        "Lebegő polcok és könyvespolc",
        "Dizájner csillár / állólámpa",
        "Puha, nagyméretű szőnyeg"
    )

    val quickCustomPrompts: List<String> = listOf(
        "Legyen világosabb és napfényesebb a szoba.",
        "Használj több természetes fát és meleg textíliát.",
        "Adj hozzá egy nagy, kényelmes sarokkanapét.",
        "Tedd luxusabbá arany és márvány részletekkel.",
        "Tartsd meg a fő elrendezést, de frissítsd a színeket."
    )

    val sampleDemoAnalysis = RoomAnalysis(
        roomType = "Nappali (Living Room)",
        estimatedDimensions = "Kb. 4.2 m × 5.4 m (~22.5 m²), belmagasság kb. 2.75 m (fénykép alapú becslés)",
        existingFurniture = listOf(
            "Kétszemélyes világos szövetkanapé a hátsó falnál",
            "Sötétbarna kárpitozott fotel",
            "Kisméretű fa lerakóasztal és állólámpa"
        ),
        wallColors = "Matt törtfehér / enyhén kopott világos vakolat",
        flooring = "Hagyományos lakkozott fenyő hajópadló meleg tónusban",
        windowsAndDoors = "Bal oldalon nagyméretű, háromosztású fehér fa ablak; hátsó falon nyitott beltéri ajtó, jobb oldalon fa bejárati ajtó",
        lightingConditions = "Kiváló természetes oldalfény balról (nappali megvilágítás), központi függesztett mennyezeti lámpával",
        existingLayout = "Tágas középső közlekedőterület, a bútorok a hátsó és jobb oldali fal mentén helyezkednek el",
        designRecommendation = "Az ablakok kiváló természetes fényt biztosítanak. Egy ívelt bouclé kanapé, nagyméretű strukturált gyapjúszőnyeg, meleg tölgy dohányzóasztal és sárgaréz függesztéklámpa látványosan kiemelné a teret a szoba építészeti adottságainak megőrzése mellett.",
        isDemoSample = true
    )
}
