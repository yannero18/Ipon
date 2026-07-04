package com.ipon.app.util

import android.content.Context

/**
 * Whether the person has seen the onboarding intro. A single boolean is
 * not worth a Room table/migration -- SharedPreferences is the right-sized
 * tool here, used nowhere else in the app specifically so it stays obvious
 * that this is the one exception to "everything lives in Room."
 */
class OnboardingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasSeenOnboarding(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)

    fun markOnboardingSeen() {
        prefs.edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
    }

    fun getAccountName(): String = prefs.getString(KEY_ACCOUNT_NAME, "Yannero") ?: "Yannero"

    fun saveAccountName(name: String) {
        prefs.edit().putString(KEY_ACCOUNT_NAME, name).apply()
    }

    companion object {
        private const val PREFS_NAME = "ipon_onboarding_prefs"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_ACCOUNT_NAME = "account_name"
    }
}
