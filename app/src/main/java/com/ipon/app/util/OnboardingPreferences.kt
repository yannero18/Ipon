package com.ipon.app.util

import android.content.Context

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

    // --- PAYDAY PREFERENCES ---
    fun getPaydays(): List<Int> {
        val daysString = prefs.getString(KEY_PAYDAYS, "15,30") ?: "15,30"
        return daysString.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
    
    fun savePaydays(days: List<Int>) {
        prefs.edit().putString(KEY_PAYDAYS, days.joinToString(",")).apply()
    }

    companion object {
        private const val PREFS_NAME = "ipon_onboarding_prefs"
        private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        private const val KEY_ACCOUNT_NAME = "account_name"
        private const val KEY_PAYDAYS = "paydays"
    }
}