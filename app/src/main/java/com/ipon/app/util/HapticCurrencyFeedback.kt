package com.ipon.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ipon.app.util.Money

/**
 * Section 5: "Haptic Currency Feedback" -- a light tick for small purchases,
 * a heavier double-pulse for amounts crossing a user-defined threshold.
 *
 * This is a real implementation against Android's VibrationEffect API
 * (API 26+), not a stub -- predictive/composition haptics
 * (VibrationEffect.compose) are API 30+, so this falls back to amplitude
 * + timing-based effects for wider device coverage rather than requiring
 * the newest API for a core interaction.
 */
class HapticCurrencyFeedback(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Call after a transaction is successfully saved. [amount] and
     * [largeAmountThreshold] are both [Money] so the comparison happens in
     * minor units -- never converted through a Double for this check.
     */
    fun onTransactionSaved(amount: Money, largeAmountThreshold: Money) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        if (amount.minorUnits.let { if (it < 0) -it else it } >= largeAmountThreshold.minorUnits) {
            // Heavier double-pulse for large amounts.
            val timings = longArrayOf(0, 40, 60, 40)
            val amplitudes = intArrayOf(0, 180, 0, 220)
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            // Light tick for ordinary, everyday amounts.
            v.vibrate(VibrationEffect.createOneShot(20, 80))
        }
    }

    /** Distinct sharper click for corrections/deletes, per Section 5. */
    fun onDeleteConfirmed() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        v.vibrate(VibrationEffect.createOneShot(15, 255))
    }
}
