/*
 * Nextcloud Talk - Android Client
 * Enhanced Audio Settings Activity
 *
 * SPDX-FileCopyrightText: 2025 Talk Enhanced Team
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.nextcloud.talk.R
import com.nextcloud.talk.activities.BaseActivity
import com.nextcloud.talk.webrtc.AudioProcessor

/**
 * Activity для настройки качества аудио во время звонков
 */
class AudioSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_settings)

        setupActionBar()

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, AudioSettingsFragment())
                .commit()
        }
    }

    private fun setupActionBar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.nc_audio_settings_title)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Fragment с настройками аудио
     */
    class AudioSettingsFragment : PreferenceFragmentCompat() {

        private lateinit var audioProcessor: AudioProcessor

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.audio_preferences, rootKey)
            
            audioProcessor = AudioProcessor(requireContext())
            setupPreferences()
        }

        private fun setupPreferences() {
            // Кнопка сброса настроек
            findPreference<Preference>("audio_reset")?.setOnPreferenceClickListener {
                showResetDialog()
                true
            }

            // Настройка слушателей для обновления summary
            val preferences = listOf(
                "noise_suppression_enabled",
                "echo_cancellation_enabled", 
                "auto_gain_control_enabled",
                "high_pass_filter_enabled",
                "typing_noise_detection_enabled"
            )

            preferences.forEach { key ->
                findPreference<SwitchPreferenceCompat>(key)?.setOnPreferenceChangeListener { _, newValue ->
                    // Обновляем summary в info preference
                    updateAudioInfoSummary()
                    true
                }
            }

            // Инициализируем summary
            updateAudioInfoSummary()
        }

        private fun updateAudioInfoSummary() {
            val infoPreference = findPreference<Preference>("audio_info")
            val summary = audioProcessor.getAudioSettingsSummary()
            
            val fullSummary = "Active features: $summary\n\n" +
                    "All settings apply to the next call. " +
                    "For optimal quality it's recommended to keep all options enabled."
            
            infoPreference?.summary = fullSummary
        }

        private fun showResetDialog() {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.nc_audio_reset_confirmation_title)
                .setMessage(R.string.nc_audio_reset_confirmation_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    resetToDefaults()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        private fun resetToDefaults() {
            // Сбрасываем настройки через AudioProcessor
            audioProcessor.resetToDefaults()

            // Обновляем UI
            findPreference<SwitchPreferenceCompat>("noise_suppression_enabled")?.isChecked = true
            findPreference<SwitchPreferenceCompat>("echo_cancellation_enabled")?.isChecked = true
            findPreference<SwitchPreferenceCompat>("auto_gain_control_enabled")?.isChecked = true
            findPreference<SwitchPreferenceCompat>("high_pass_filter_enabled")?.isChecked = true
            findPreference<SwitchPreferenceCompat>("typing_noise_detection_enabled")?.isChecked = true

            updateAudioInfoSummary()

            // Показываем сообщение об успехе
            AlertDialog.Builder(requireContext())
                .setTitle(android.R.string.ok)
                .setMessage("Audio settings have been reset to recommended values. Changes will take effect on the next call.")
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}
