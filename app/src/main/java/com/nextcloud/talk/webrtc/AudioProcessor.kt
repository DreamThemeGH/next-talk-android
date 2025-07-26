/*
 * Nextcloud Talk - Android Client
 * Enhanced Audio Processing
 *
 * SPDX-FileCopyrightText: 2025 Talk Enhanced Team
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.webrtc

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import org.webrtc.MediaConstraints

/**
 * AudioProcessor manages advanced audio processing features
 * including noise suppression, echo cancellation, and auto gain control
 */
class AudioProcessor(private val context: Context) {

    companion object {
        private const val TAG = "AudioProcessor"
        
        // Preference keys
        const val PREF_NOISE_SUPPRESSION_ENABLED = "noise_suppression_enabled"
        const val PREF_ECHO_CANCELLATION_ENABLED = "echo_cancellation_enabled"
        const val PREF_AUTO_GAIN_CONTROL_ENABLED = "auto_gain_control_enabled"
        const val PREF_HIGH_PASS_FILTER_ENABLED = "high_pass_filter_enabled"
        const val PREF_TYPING_NOISE_DETECTION_ENABLED = "typing_noise_detection_enabled"
        
        // Default values - все включено для лучшего качества
        private const val DEFAULT_NOISE_SUPPRESSION = true
        private const val DEFAULT_ECHO_CANCELLATION = true
        private const val DEFAULT_AUTO_GAIN_CONTROL = true
        private const val DEFAULT_HIGH_PASS_FILTER = true
        private const val DEFAULT_TYPING_NOISE_DETECTION = true
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * Применяет все аудио настройки к MediaConstraints
     */
    fun applyAudioConstraints(constraints: MediaConstraints) {
        Log.d(TAG, "Applying audio processing constraints")

        // Очищаем существующие аудио ограничения
        clearAudioConstraints(constraints)

        // Применяем настройки пользователя
        if (isNoiseSuppressionEnabled()) {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            Log.d(TAG, "✅ Noise Suppression enabled")
        } else {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "false"))
            Log.d(TAG, "❌ Noise Suppression disabled")
        }

        if (isEchoCancellationEnabled()) {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            Log.d(TAG, "✅ Echo Cancellation enabled")
        } else {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "false"))
            Log.d(TAG, "❌ Echo Cancellation disabled")
        }

        if (isAutoGainControlEnabled()) {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            Log.d(TAG, "✅ Auto Gain Control enabled")
        } else {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "false"))
            Log.d(TAG, "❌ Auto Gain Control disabled")
        }

        if (isHighPassFilterEnabled()) {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            Log.d(TAG, "✅ High Pass Filter enabled")
        } else {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "false"))
            Log.d(TAG, "❌ High Pass Filter disabled")
        }

        if (isTypingNoiseDetectionEnabled()) {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))
            Log.d(TAG, "✅ Typing Noise Detection enabled")
        } else {
            constraints.mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "false"))
            Log.d(TAG, "❌ Typing Noise Detection disabled")
        }

        Log.d(TAG, "Audio processing constraints applied successfully")
    }

    /**
     * Очищает аудио ограничения из MediaConstraints
     */
    private fun clearAudioConstraints(constraints: MediaConstraints) {
        val iterator = constraints.mandatory.iterator()
        while (iterator.hasNext()) {
            val pair = iterator.next()
            if (pair.key.startsWith("goog") && isAudioConstraint(pair.key)) {
                iterator.remove()
            }
        }
    }

    /**
     * Проверяет, является ли ключ аудио ограничением
     */
    private fun isAudioConstraint(key: String): Boolean {
        return key in listOf(
            "googEchoCancellation",
            "googNoiseSuppression", 
            "googAutoGainControl",
            "googHighpassFilter",
            "googTypingNoiseDetection"
        )
    }

    // Геттеры для настроек
    fun isNoiseSuppressionEnabled(): Boolean {
        return prefs.getBoolean(PREF_NOISE_SUPPRESSION_ENABLED, DEFAULT_NOISE_SUPPRESSION)
    }

    fun isEchoCancellationEnabled(): Boolean {
        return prefs.getBoolean(PREF_ECHO_CANCELLATION_ENABLED, DEFAULT_ECHO_CANCELLATION)
    }

    fun isAutoGainControlEnabled(): Boolean {
        return prefs.getBoolean(PREF_AUTO_GAIN_CONTROL_ENABLED, DEFAULT_AUTO_GAIN_CONTROL)
    }

    fun isHighPassFilterEnabled(): Boolean {
        return prefs.getBoolean(PREF_HIGH_PASS_FILTER_ENABLED, DEFAULT_HIGH_PASS_FILTER)
    }

    fun isTypingNoiseDetectionEnabled(): Boolean {
        return prefs.getBoolean(PREF_TYPING_NOISE_DETECTION_ENABLED, DEFAULT_TYPING_NOISE_DETECTION)
    }

    // Сеттеры для настроек
    fun setNoiseSuppressionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_NOISE_SUPPRESSION_ENABLED, enabled).apply()
        Log.d(TAG, "Noise Suppression set to: $enabled")
    }

    fun setEchoCancellationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_ECHO_CANCELLATION_ENABLED, enabled).apply()
        Log.d(TAG, "Echo Cancellation set to: $enabled")
    }

    fun setAutoGainControlEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_AUTO_GAIN_CONTROL_ENABLED, enabled).apply()
        Log.d(TAG, "Auto Gain Control set to: $enabled")
    }

    fun setHighPassFilterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_HIGH_PASS_FILTER_ENABLED, enabled).apply()
        Log.d(TAG, "High Pass Filter set to: $enabled")
    }

    fun setTypingNoiseDetectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_TYPING_NOISE_DETECTION_ENABLED, enabled).apply()
        Log.d(TAG, "Typing Noise Detection set to: $enabled")
    }

    /**
     * Получает краткую информацию о текущих настройках
     */
    fun getAudioSettingsSummary(): String {
        val settings = mutableListOf<String>()
        
        if (isNoiseSuppressionEnabled()) settings.add("Шумоподавление")
        if (isEchoCancellationEnabled()) settings.add("Эхоподавление")
        if (isAutoGainControlEnabled()) settings.add("Авто усиление")
        if (isHighPassFilterEnabled()) settings.add("ВЧ фильтр")
        if (isTypingNoiseDetectionEnabled()) settings.add("Подавление печати")

        return if (settings.isEmpty()) {
            "Все отключено"
        } else {
            settings.joinToString(", ")
        }
    }

    /**
     * Сброс всех настроек к значениям по умолчанию
     */
    fun resetToDefaults() {
        Log.d(TAG, "Resetting audio settings to defaults")
        
        prefs.edit().apply {
            putBoolean(PREF_NOISE_SUPPRESSION_ENABLED, DEFAULT_NOISE_SUPPRESSION)
            putBoolean(PREF_ECHO_CANCELLATION_ENABLED, DEFAULT_ECHO_CANCELLATION)
            putBoolean(PREF_AUTO_GAIN_CONTROL_ENABLED, DEFAULT_AUTO_GAIN_CONTROL)
            putBoolean(PREF_HIGH_PASS_FILTER_ENABLED, DEFAULT_HIGH_PASS_FILTER)
            putBoolean(PREF_TYPING_NOISE_DETECTION_ENABLED, DEFAULT_TYPING_NOISE_DETECTION)
            apply()
        }
    }
}
