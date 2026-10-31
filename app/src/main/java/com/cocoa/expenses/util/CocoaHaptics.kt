package com.cocoa.expenses.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

object CocoaHaptics {

    private fun vibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * 1. Light Tick (CLOCK_TICK / VIRTUAL_KEY):
     * Switching months on the top carousel, toggling filter chips, scrolling date pickers.
     */
    fun lightTick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * 2. Medium Confirm (CONFIRM / Double-Crisp Pulse):
     * Marking an expense as Paid/Done via swipe-right, saving a new transaction,
     * logging a fuel refill, or completing an Excel sheet import.
     */
    fun mediumConfirm(view: View, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            val vib = vibrator(context)
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 22, 45, 30), intArrayOf(0, 160, 0, 220), -1))
        }
    }

    /**
     * 3. Heavy Warning (REJECT / Triple-Buzz):
     * Crossing 100% of the Monthly Budget (e.g., Remaining Balance goes negative like -₹106),
     * exceeding the Fuel Sub-Budget (-₹79.33), or deleting a record.
     */
    fun heavyWarning(view: View, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
        val vib = vibrator(context)
        vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 45, 40, 45, 40, 60), intArrayOf(0, 255, 0, 255, 0, 255), -1))
    }
}
