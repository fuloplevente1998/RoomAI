package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

// Fallback Firebase configuration from provisioned google-services.json so external
// CI/CD APK builds (e.g. GitHub Actions where google-services.json might be absent)
// always initialize Default FirebaseApp reliably on physical phones.
private const val FALLBACK_PROJECT_ID = "gen-lang-client-0310222616"
private const val FALLBACK_APPLICATION_ID = "1:203039061226:android:f5c4d77ed852be622b9393"
private const val FALLBACK_API_KEY = "AIzaSyAsBdsu1vYK0WfOlK8dGChfyo1BaQWMvuc"
private const val FALLBACK_GCM_SENDER_ID = "203039061226"
private const val FALLBACK_STORAGE_BUCKET = "gen-lang-client-0310222616.firebasestorage.app"

/**
 * Guarantees that the Default [FirebaseApp] is initialized in the current process.
 * First attempts standard resource-based initialization (`FirebaseApp.initializeApp(context)`),
 * and if `google-services.json` resources were stripped during an external build,
 * initializes explicitly with [FirebaseOptions].
 */
fun ensureFirebaseInitialized(context: Context): FirebaseApp {
    val appContext = context.applicationContext ?: context
    val existingApps = FirebaseApp.getApps(appContext)
    if (existingApps.isNotEmpty()) {
        return FirebaseApp.getInstance()
    }

    val autoApp = FirebaseApp.initializeApp(appContext)
    if (autoApp != null) {
        return autoApp
    }

    Log.w(TAG, "Standard FirebaseApp.initializeApp(context) returned null; initializing with explicit FirebaseOptions.")
    val options = FirebaseOptions.Builder()
        .setProjectId(FALLBACK_PROJECT_ID)
        .setApplicationId(FALLBACK_APPLICATION_ID)
        .setApiKey(FALLBACK_API_KEY)
        .setGcmSenderId(FALLBACK_GCM_SENDER_ID)
        .setStorageBucket(FALLBACK_STORAGE_BUCKET)
        .build()

    return FirebaseApp.initializeApp(appContext, options)
}

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        // 1. Ensure FirebaseApp is initialized (with automatic fallback for external APK builds)
        val firebaseApp = ensureFirebaseInitialized(context)

        if (BuildConfig.DEBUG) {
            // 2. Debug: Read debug token from Intent extra
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)

            if (!debugToken.isNullOrEmpty()) {
                val persistenceKey = firebaseApp.persistenceKey
                val prefs = context.getSharedPreferences(
                    "$DEBUG_STORE_PREFIX$persistenceKey",
                    Context.MODE_PRIVATE
                )
                // CRITICAL: MUST use commit() instead of apply() so the secret is written
                // synchronously before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                Log.d(TAG, "App Check debug secret configured successfully.")
            } else {
                Log.w(TAG, "No debug token found in Intent extra.")
            }

            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG, "DebugAppCheckProviderFactory installed.")
        } else {
            // 3. Release: Look up recaptcha_site_key safely without throwing Resources.NotFoundException
            val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", context.packageName)
            if (resId != 0) {
                val siteKey = context.getString(resId)
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                        RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                    )
                    Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
                } else {
                    Log.e(TAG, "recaptcha_site_key resource string is blank.")
                }
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found in package.")
            }
        }
    } catch (e: Exception) {
        // Log explicitly to keep App Check attestation failures visible
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}
