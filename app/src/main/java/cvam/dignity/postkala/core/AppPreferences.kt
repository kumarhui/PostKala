package cvam.dignity.postkala.core

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("postkala_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ADVANCED_FEATURES_ENABLED = "advanced_features_enabled"
    }

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var isAdvancedFeaturesEnabled: Boolean
        get() = prefs.getBoolean(KEY_ADVANCED_FEATURES_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ADVANCED_FEATURES_ENABLED, value).apply()
}