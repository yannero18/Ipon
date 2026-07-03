package com.ipon.app.util

import android.content.Context

/**
 * Legacy wrapper for Haptic Currency Feedback.
 * Delegates all actions to the unified [HapticFeedbackManager].
 */
class HapticCurrencyFeedback(context: Context) {

    private val manager = HapticFeedbackManager(context)

    fun onTransactionSaved(amount: Money, largeAmountThreshold: Money) {
        manager.onTransactionSaved(amount, largeAmountThreshold)
    }

    fun onDeleteConfirmed() {
        manager.onDeleteConfirmed()
    }

    fun playCoinDropSound() {
        manager.playCoinDropSound()
    }
}
