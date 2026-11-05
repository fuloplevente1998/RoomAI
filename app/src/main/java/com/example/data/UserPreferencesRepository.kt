package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.SubscriptionPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "roomai_user_prefs")

data class UserProfileState(
    val displayName: String = "Kovács Anna",
    val email: String = "anna.kovacs@roomai.hu",
    val subscriptionPlan: SubscriptionPlan = SubscriptionPlan.FREE,
    val generationsUsedCount: Int = 0,
    val freeGenerationLimit: Int = 2,
    val selectedLanguage: AppLanguage = AppLanguage.HUNGARIAN,
    val highResExportEnabled: Boolean = false,
    val hasConsentedPhotoUpload: Boolean = true
) {
    val isPremium: Boolean
        get() = subscriptionPlan == SubscriptionPlan.MONTHLY || subscriptionPlan == SubscriptionPlan.ANNUAL

    val remainingFreeGenerations: Int
        get() = (freeGenerationLimit - generationsUsedCount).coerceAtLeast(0)

    val canGenerateAiImage: Boolean
        get() = isPremium || generationsUsedCount < freeGenerationLimit
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val EMAIL = stringPreferencesKey("email")
        val SUBSCRIPTION_PLAN = stringPreferencesKey("subscription_plan")
        val GENERATIONS_USED = intPreferencesKey("generations_used")
        val LANGUAGE = stringPreferencesKey("selected_language")
        val HIGH_RES_EXPORT = booleanPreferencesKey("high_res_export")
        val PHOTO_CONSENT = booleanPreferencesKey("photo_consent")
    }

    val userProfileFlow: Flow<UserProfileState> = context.dataStore.data.map { prefs ->
        val planId = prefs[Keys.SUBSCRIPTION_PLAN] ?: SubscriptionPlan.FREE.id
        val plan = SubscriptionPlan.entries.find { it.id == planId } ?: SubscriptionPlan.FREE
        val langCode = prefs[Keys.LANGUAGE] ?: AppLanguage.HUNGARIAN.code
        val lang = AppLanguage.entries.find { it.code == langCode } ?: AppLanguage.HUNGARIAN

        UserProfileState(
            displayName = prefs[Keys.DISPLAY_NAME] ?: "Kovács Anna",
            email = prefs[Keys.EMAIL] ?: "anna.kovacs@roomai.hu",
            subscriptionPlan = plan,
            generationsUsedCount = prefs[Keys.GENERATIONS_USED] ?: 0,
            selectedLanguage = lang,
            highResExportEnabled = prefs[Keys.HIGH_RES_EXPORT] ?: (plan != SubscriptionPlan.FREE),
            hasConsentedPhotoUpload = prefs[Keys.PHOTO_CONSENT] ?: true
        )
    }

    suspend fun setSubscriptionPlan(plan: SubscriptionPlan) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SUBSCRIPTION_PLAN] = plan.id
            prefs[Keys.HIGH_RES_EXPORT] = (plan != SubscriptionPlan.FREE)
        }
    }

    suspend fun incrementGenerationCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.GENERATIONS_USED] ?: 0
            prefs[Keys.GENERATIONS_USED] = current + 1
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun updateProfile(name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DISPLAY_NAME] = name
            prefs[Keys.EMAIL] = email
        }
    }

    suspend fun resetAccountData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
