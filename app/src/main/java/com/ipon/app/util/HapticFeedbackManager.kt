package com.ipon.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Manages all haptic and audio feedback patterns for user actions across the app.
 * Adheres to Material Design guidelines and Section 5 of the specification.
 */
class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Successful save action haptic feedback:
     * - Plays a programmatic coin drop chime in the background.
     * - Triggers different vibration patterns based on the saved amount:
     *   - Light tick for small or zero amounts.
     *   - Heavy double-pulse for amounts crossing the specified [largeAmountThreshold].
     */
    fun onTransactionSaved(amount: Money, largeAmountThreshold: Money = Money.ZERO) {
        playCoinDropSound()

        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val isLarge = amount.minorUnits.let { if (it < 0) -it else it } >= largeAmountThreshold.minorUnits && largeAmountThreshold.minorUnits > 0
        if (isLarge) {
            // Heavier double-pulse for large amounts
            val timings = longArrayOf(0, 40, 60, 40)
            val amplitudes = intArrayOf(0, 180, 0, 220)
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            // Light tick for ordinary, everyday amounts
            v.vibrate(VibrationEffect.createOneShot(20, 80))
        }
    }

    /**
     * Distinct sharper click for deletion or cancel confirmation.
     */
    fun onDeleteConfirmed() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        v.vibrate(VibrationEffect.createOneShot(15, 255))
    }

    /**
     * Distinct urgent triple pulse for validation errors (e.g. missing amount, bad fields).
     */
    fun onValidationError() {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        
        // Triple short vibration pulses with high amplitude
        val timings = longArrayOf(0, 50, 40, 50, 40, 50)
        val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
        v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    /**
     * Programmatic "coin drop" arpeggio synthesizer (C6 -> E6 with exponential decay)
     */
    fun playCoinDropSound() {
        Thread {
            val sampleRate = 44100
            val duration1 = 0.08f // seconds
            val duration2 = 0.20f // seconds
            val numSamples1 = (sampleRate * duration1).toInt()
            val numSamples2 = (sampleRate * duration2).toInt()
            val totalSamples = numSamples1 + numSamples2
            val generatedSnd = ShortArray(totalSamples)

            val freq1 = 1046.5 // Hz (C6)
            val freq2 = 1318.5 // Hz (E6)

            // Note 1: Fast attack, linear decay
            for (i in 0 until numSamples1) {
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * freq1 * t
                val amplitude = 32767.0 * (1.0 - t / duration1) * 0.7
                generatedSnd[i] = (kotlin.math.sin(angle) * amplitude).toInt().toShort()
            }

            // Note 2: Exponential decay for a lingering, physical chime
            for (i in 0 until numSamples2) {
                val t = i.toDouble() / sampleRate
                val angle = 2.0 * Math.PI * freq2 * t
                val amplitude = 32767.0 * kotlin.math.exp(-6.0 * t) * 0.9
                generatedSnd[numSamples1 + i] = (kotlin.math.sin(angle) * amplitude).toInt().toShort()
            }

            try {
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(totalSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, totalSamples)
                audioTrack.play()
                
                Thread.sleep(((duration1 + duration2) * 1000 + 100).toLong())
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // ignore
            }
        }.start()
    }
}
