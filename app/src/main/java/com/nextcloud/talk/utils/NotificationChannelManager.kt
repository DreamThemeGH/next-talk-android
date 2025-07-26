/*
 * Nextcloud Talk - Android Client
 * Individual notification channels system
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.nextcloud.talk.R
import com.nextcloud.talk.utils.preferences.AppPreferences

/**
 * Notification Channel Manager for Nextcloud Talk
 * Provides individual sound notifications for each message/chat
 */
class NotificationChannelManager(
    private val context: Context,
    private val appPreferences: AppPreferences
) {
    
    private val notificationManager = NotificationManagerCompat.from(context)
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    
    companion object {
        private const val TAG = "NotificationChannelManager"
        
        // Individual notification channels
        const val CHANNEL_MESSAGES_INDIVIDUAL = "messages_individual_v1"
        const val CHANNEL_CALLS_INDIVIDUAL = "calls_individual_v1"
        const val CHANNEL_INDIVIDUAL_CHAT_PREFIX = "chat_individual_"
        
        // Preferences keys
        const val PREF_DISABLE_GROUPING = "disable_notification_grouping"
        const val PREF_INDIVIDUAL_SOUNDS = "individual_chat_sounds"
        const val PREF_SMART_NOTIFICATIONS = "smart_notifications"
        const val PREF_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        const val PREF_QUIET_HOURS_START = "quiet_hours_start"
        const val PREF_QUIET_HOURS_END = "quiet_hours_end"
        
        // Default sound URIs
        const val DEFAULT_ENHANCED_MESSAGE_SOUND = 
            "android.resource://${com.nextcloud.talk.BuildConfig.APPLICATION_ID}/raw/enhanced_message_sound"
    }
    
    /**
     * Data class for notification settings per chat
     */
    data class ChatNotificationSettings(
        val soundEnabled: Boolean = true,
        val vibrationEnabled: Boolean = true,
        val customSoundUri: String? = null,
        val priority: Int = NotificationCompat.PRIORITY_HIGH,
        val allowGrouping: Boolean = false
    )
    
    fun setupEnhancedNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Setting up enhanced notification channels")
            
            // Enhanced messages channel
            createEnhancedMessagesChannel()
            
            // Enhanced calls channel 
            createEnhancedCallsChannel()
            
            Log.d(TAG, "Enhanced notification channels created successfully")
        }
    }
    
    private fun createEnhancedMessagesChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_MESSAGES_ENHANCED,
                context.getString(R.string.enhanced_messages_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.enhanced_messages_channel_description)
                enableSound(true)
                setSound(getDefaultMessageSoundUri(), getDefaultAudioAttributes())
                enableVibration(true)
                setShowBadge(true)
                lightColor = context.getColor(R.color.colorPrimary)
                enableLights(true)
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Created enhanced messages channel: $CHANNEL_MESSAGES_ENHANCED")
        }
    }
    
    private fun createEnhancedCallsChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_CALLS_ENHANCED,
                context.getString(R.string.enhanced_calls_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.enhanced_calls_channel_description)
                enableSound(true)
                setSound(getDefaultCallSoundUri(), getCallAudioAttributes())
                enableVibration(true)
                setBypassDnd(true) // Important for calls
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Created enhanced calls channel: $CHANNEL_CALLS_ENHANCED")
        }
    }
    
    /**
     * Creates unique notification channel for individual chat
     * This ensures each chat can have its own sound settings
     */
    fun createIndividualChatChannel(
        conversationToken: String,
        chatName: String,
        settings: ChatNotificationSettings = ChatNotificationSettings()
    ): String {
        val channelId = "$CHANNEL_INDIVIDUAL_CHAT_PREFIX$conversationToken"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Check if grouping is disabled or this chat has individual settings
            val shouldCreateIndividualChannel = !settings.allowGrouping || 
                    preferences.getBoolean(PREF_DISABLE_GROUPING, false) ||
                    preferences.getBoolean(PREF_INDIVIDUAL_SOUNDS, false)
            
            if (shouldCreateIndividualChannel) {
                val soundUri = if (settings.customSoundUri != null) {
                    Uri.parse(settings.customSoundUri)
                } else {
                    getDefaultMessageSoundUri()
                }
                
                val channel = NotificationChannel(
                    channelId,
                    "💬 $chatName",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Уведомления для чата: $chatName"
                    enableSound(settings.soundEnabled)
                    if (settings.soundEnabled) {
                        setSound(soundUri, getDefaultAudioAttributes())
                    }
                    enableVibration(settings.vibrationEnabled)
                    setShowBadge(true)
                    lightColor = context.getColor(R.color.colorPrimary)
                    enableLights(true)
                }
                
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Created individual chat channel: $channelId for chat: $chatName")
                return channelId
            }
        }
        
        // Fallback to enhanced messages channel
        return CHANNEL_MESSAGES_ENHANCED
    }
    
    /**
     * Creates notification with enhanced sound support
     */
    fun createEnhancedNotification(
        conversationToken: String,
        chatName: String,
        messageContent: String,
        senderName: String,
        settings: ChatNotificationSettings = ChatNotificationSettings()
    ): Notification {
        
        val channelId = createIndividualChatChannel(conversationToken, chatName, settings)
        val uniqueNotificationId = generateUniqueNotificationId()
        
        Log.d(TAG, "Creating enhanced notification for chat: $chatName, channel: $channelId")
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(chatName)
            .setContentText("$senderName: $messageContent")
            .setSmallIcon(R.drawable.ic_logo)
            .setPriority(settings.priority)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
        
        // Enhanced sound handling
        if (settings.soundEnabled && !isInQuietHours()) {
            val soundUri = if (settings.customSoundUri != null) {
                Uri.parse(settings.customSoundUri)
            } else {
                getDefaultMessageSoundUri()
            }
            
            // Force sound even if channel already has it
            builder.setSound(soundUri)
            
            // For older Android versions, force notification defaults
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setDefaults(0) // Clear defaults
                if (settings.vibrationEnabled) {
                    builder.setVibrate(longArrayOf(0, 250, 250, 250))
                }
            }
        }
        
        // Prevent grouping if disabled
        if (!settings.allowGrouping || preferences.getBoolean(PREF_DISABLE_GROUPING, false)) {
            builder.setGroup("unique_group_$uniqueNotificationId")
            builder.setGroupSummary(false)
        }
        
        // Add action buttons for quick reply
        addQuickReplyActions(builder, conversationToken)
        
        val notification = builder.build()
        
        // Additional flags for enhanced behavior
        if (settings.soundEnabled && !isInQuietHours()) {
            notification.flags = notification.flags or Notification.FLAG_INSISTENT
        }
        
        Log.d(TAG, "Enhanced notification created successfully")
        return notification
    }
    
    /**
     * Generates unique notification ID to prevent grouping
     */
    private fun generateUniqueNotificationId(): Int {
        return (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
    }
    
    /**
     * Check if current time is in quiet hours
     */
    private fun isInQuietHours(): Boolean {
        if (!preferences.getBoolean(PREF_QUIET_HOURS_ENABLED, false)) {
            return false
        }
        
        // TODO: Implement quiet hours logic
        // For now, return false
        return false
    }
    
    /**
     * Add quick reply and other action buttons
     */
    private fun addQuickReplyActions(builder: NotificationCompat.Builder, conversationToken: String) {
        // TODO: Implement quick reply actions
        // This will be added in the next iteration
    }
    
    /**
     * Get notification settings for specific chat
     */
    fun getChatNotificationSettings(conversationToken: String): ChatNotificationSettings {
        val chatPrefs = context.getSharedPreferences("chat_notifications", Context.MODE_PRIVATE)
        
        return ChatNotificationSettings(
            soundEnabled = chatPrefs.getBoolean("${conversationToken}_sound", true),
            vibrationEnabled = chatPrefs.getBoolean("${conversationToken}_vibration", true),
            customSoundUri = chatPrefs.getString("${conversationToken}_custom_sound", null),
            priority = chatPrefs.getInt("${conversationToken}_priority", NotificationCompat.PRIORITY_HIGH),
            allowGrouping = chatPrefs.getBoolean("${conversationToken}_allow_grouping", false)
        )
    }
    
    /**
     * Save notification settings for specific chat
     */
    fun saveChatNotificationSettings(conversationToken: String, settings: ChatNotificationSettings) {
        val chatPrefs = context.getSharedPreferences("chat_notifications", Context.MODE_PRIVATE)
        
        chatPrefs.edit()
            .putBoolean("${conversationToken}_sound", settings.soundEnabled)
            .putBoolean("${conversationToken}_vibration", settings.vibrationEnabled)
            .putString("${conversationToken}_custom_sound", settings.customSoundUri)
            .putInt("${conversationToken}_priority", settings.priority)
            .putBoolean("${conversationToken}_allow_grouping", settings.allowGrouping)
            .apply()
        
        Log.d(TAG, "Saved notification settings for chat: $conversationToken")
    }
    
    // Audio attributes and sound URIs
    private fun getDefaultAudioAttributes() = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
        .build()
    
    private fun getCallAudioAttributes() = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST)
        .build()
    
    private fun getDefaultMessageSoundUri(): Uri {
        return try {
            Uri.parse(DEFAULT_ENHANCED_MESSAGE_SOUND)
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse enhanced sound URI, falling back to default")
            Uri.parse(NotificationUtils.DEFAULT_MESSAGE_RINGTONE_URI)
        }
    }
    
    private fun getDefaultCallSoundUri(): Uri {
        return Uri.parse(NotificationUtils.DEFAULT_CALL_RINGTONE_URI)
    }
}
