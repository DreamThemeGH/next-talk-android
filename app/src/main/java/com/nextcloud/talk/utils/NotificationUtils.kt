/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import coil.executeBlocking
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.bluelinelabs.logansquare.LoganSquare
import com.nextcloud.talk.BuildConfig
import com.nextcloud.talk.R
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.models.RingtoneSettings
import com.nextcloud.talk.utils.bundle.BundleKeys
import com.nextcloud.talk.utils.preferences.AppPreferences
import java.io.IOException

@Suppress("TooManyFunctions")
object NotificationUtils {

    const val TAG = "NotificationUtils"

    enum class NotificationChannels {
        NOTIFICATION_CHANNEL_MESSAGES_V4,
        NOTIFICATION_CHANNEL_CALLS_V4,
        NOTIFICATION_CHANNEL_UPLOADS
    }

    const val DEFAULT_CALL_RINGTONE_URI =
        "android.resource://" + BuildConfig.APPLICATION_ID + "/raw/librem_by_feandesign_call"
    const val DEFAULT_MESSAGE_RINGTONE_URI =
        "android.resource://" + BuildConfig.APPLICATION_ID + "/raw/librem_by_feandesign_message"

    // RemoteInput key - used for replies sent directly from notification
    const val KEY_DIRECT_REPLY = "key_direct_reply"

    // notification group keys
    const val KEY_UPLOAD_GROUP = "com.nextcloud.talk.utils.KEY_UPLOAD_GROUP"
    const val GROUP_SUMMARY_NOTIFICATION_ID = -1
    
    // Smart notification preferences (like WhatsApp)
    const val PREF_SMART_GROUPING_ENABLED = "smart_grouping_enabled"
    const val PREF_REPEAT_NOTIFICATIONS = "repeat_notifications_enabled"
    const val PREF_GROUPING_TIMEOUT_MINUTES = "grouping_timeout_minutes"
    const val DEFAULT_GROUPING_TIMEOUT_MINUTES = 3 // 3 minutes default

    private fun createNotificationChannel(
        context: Context,
        notificationChannel: Channel,
        sound: Uri?,
        audioAttributes: AudioAttributes?
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (
            notificationManager.getNotificationChannel(notificationChannel.id) == null
        ) {
            val importance = if (notificationChannel.isImportant) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_LOW
            }

            val channel = NotificationChannel(
                notificationChannel.id,
                notificationChannel.name,
                importance
            )

            channel.description = notificationChannel.description
            channel.enableLights(true)
            channel.lightColor = R.color.colorPrimary
            channel.setSound(sound, audioAttributes)
            channel.setBypassDnd(false)

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createCallsNotificationChannel(context: Context, appPreferences: AppPreferences) {
        val audioAttributes =
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_REQUEST)
                .build()
        val soundUri = getCallRingtoneUri(context, appPreferences)

        createNotificationChannel(
            context,
            Channel(
                NotificationChannels.NOTIFICATION_CHANNEL_CALLS_V4.name,
                context.resources.getString(R.string.nc_notification_channel_calls),
                context.resources.getString(R.string.nc_notification_channel_calls_description),
                true
            ),
            soundUri,
            audioAttributes
        )
    }

    private fun createMessagesNotificationChannel(context: Context, appPreferences: AppPreferences) {
        val audioAttributes =
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build()
        val soundUri = getMessageRingtoneUri(context, appPreferences)

        createNotificationChannel(
            context,
            Channel(
                NotificationChannels.NOTIFICATION_CHANNEL_MESSAGES_V4.name,
                context.resources.getString(R.string.nc_notification_channel_messages),
                context.resources.getString(R.string.nc_notification_channel_messages_description),
                true
            ),
            soundUri,
            audioAttributes
        )
    }

    private fun createUploadsNotificationChannel(context: Context) {
        createNotificationChannel(
            context,
            Channel(
                NotificationChannels.NOTIFICATION_CHANNEL_UPLOADS.name,
                context.resources.getString(R.string.nc_notification_channel_uploads),
                context.resources.getString(R.string.nc_notification_channel_uploads_description),
                false
            ),
            null,
            null
        )
    }

    fun registerNotificationChannels(context: Context, appPreferences: AppPreferences) {
        createCallsNotificationChannel(context, appPreferences)
        createMessagesNotificationChannel(context, appPreferences)
        createUploadsNotificationChannel(context)
        
        // Initialize smart notification preferences (like WhatsApp)
        initializeSmartNotificationPreferences(context)
    }
    
    /**
     * Initialize smart notification preferences with defaults
     */
    private fun initializeSmartNotificationPreferences(context: Context) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        
        // Set default values if not already set
        if (!prefs.contains(PREF_SMART_GROUPING_ENABLED)) {
            prefs.edit()
                .putBoolean(PREF_SMART_GROUPING_ENABLED, true) // Enable smart grouping by default
                .putBoolean(PREF_REPEAT_NOTIFICATIONS, true) // Enable repeat notifications
                .putInt(PREF_GROUPING_TIMEOUT_MINUTES, DEFAULT_GROUPING_TIMEOUT_MINUTES)
                .apply()
            
            Log.d(TAG, "Initialized smart notification preferences with defaults")
        }
    }

    fun removeOldNotificationChannels(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Current version does not use notification channel groups - delete all groups
        for (channelGroup in notificationManager.notificationChannelGroups) {
            notificationManager.deleteNotificationChannelGroup(channelGroup.id)
        }

        val channelsToKeep = NotificationChannels.values().map { it.name }

        // Delete all notification channels created by previous versions
        for (channel in notificationManager.notificationChannels) {
            if (!channelsToKeep.contains(channel.id)) {
                notificationManager.deleteNotificationChannel(channel.id)
            }
        }
    }

    private fun getNotificationChannel(context: Context, channelId: String): NotificationChannel? {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.getNotificationChannel(channelId)
    }

    private inline fun scanNotifications(
        context: Context?,
        conversationUser: User,
        callback: (
            notificationManager: NotificationManager,
            statusBarNotification: StatusBarNotification,
            notification: Notification
        ) -> Unit
    ) {
        if (conversationUser.id == -1L || context == null) {
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val statusBarNotifications = notificationManager.activeNotifications
        var notification: Notification?
        for (statusBarNotification in statusBarNotifications) {
            notification = statusBarNotification.notification

            if (
                notification != null &&
                !notification.extras.isEmpty &&
                conversationUser.id == notification.extras.getLong(BundleKeys.KEY_INTERNAL_USER_ID)
            ) {
                callback(notificationManager, statusBarNotification, notification)
            }
        }
    }

    fun cancelAllNotificationsForAccount(context: Context?, conversationUser: User) {
        scanNotifications(context, conversationUser) { notificationManager, statusBarNotification, _ ->
            notificationManager.cancel(statusBarNotification.id)
        }
    }

    fun cancelNotification(context: Context?, conversationUser: User, notificationId: Long?) {
        scanNotifications(context, conversationUser) { notificationManager, statusBarNotification, notification ->
            if (notificationId == notification.extras.getLong(BundleKeys.KEY_NOTIFICATION_ID)) {
                notificationManager.cancel(statusBarNotification.id)
            }
        }
    }

    fun findNotificationForRoom(
        context: Context?,
        conversationUser: User,
        roomTokenOrId: String
    ): StatusBarNotification? {
        scanNotifications(context, conversationUser) { _, statusBarNotification, notification ->
            if (roomTokenOrId == notification.extras.getString(BundleKeys.KEY_ROOM_TOKEN)) {
                return statusBarNotification
            }
        }
        return null
    }

    fun cancelExistingNotificationsForRoom(context: Context?, conversationUser: User, roomTokenOrId: String) {
        scanNotifications(context, conversationUser) { notificationManager, statusBarNotification, notification ->
            if (roomTokenOrId == notification.extras.getString(BundleKeys.KEY_ROOM_TOKEN) &&
                !notification.extras.getBoolean(BundleKeys.KEY_NOTIFICATION_RESTRICT_DELETION)
            ) {
                notificationManager.cancel(statusBarNotification.id)
            }
        }
    }

    fun isNotificationVisible(context: Context?, notificationId: Int): Boolean {
        var isVisible = false

        val notificationManager = context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = notificationManager.activeNotifications
        for (notification in notifications) {
            if (notification.id == notificationId) {
                isVisible = true
                break
            }
        }
        return isVisible
    }

    fun isCallsNotificationChannelEnabled(context: Context): Boolean {
        val channel = getNotificationChannel(context, NotificationChannels.NOTIFICATION_CHANNEL_CALLS_V4.name)
        if (channel != null) {
            return isNotificationChannelEnabled(channel)
        }
        return false
    }

    fun isMessagesNotificationChannelEnabled(context: Context): Boolean {
        val channel = getNotificationChannel(context, NotificationChannels.NOTIFICATION_CHANNEL_MESSAGES_V4.name)
        if (channel != null) {
            return isNotificationChannelEnabled(channel)
        }
        return false
    }

    private fun isNotificationChannelEnabled(channel: NotificationChannel): Boolean =
        channel.importance != NotificationManager.IMPORTANCE_NONE

    private fun getRingtoneUri(
        context: Context,
        ringtonePreferencesString: String?,
        defaultRingtoneUri: String,
        channelId: String
    ): Uri? {
        val channel = getNotificationChannel(context, channelId)
        if (channel != null) {
            return channel.sound
        }
        // Notification channel will not be available when starting the application for the first time.
        // Ringtone uris are required to register the notification channels -> get uri from preferences.

        return if (TextUtils.isEmpty(ringtonePreferencesString)) {
            defaultRingtoneUri.toUri()
        } else {
            try {
                val ringtoneSettings =
                    LoganSquare.parse(ringtonePreferencesString, RingtoneSettings::class.java)
                ringtoneSettings.ringtoneUri
            } catch (exception: IOException) {
                defaultRingtoneUri.toUri()
            }
        }
    }

    fun getCallRingtoneUri(context: Context, appPreferences: AppPreferences): Uri? =
        getRingtoneUri(
            context,
            appPreferences.callRingtoneUri,
            DEFAULT_CALL_RINGTONE_URI,
            NotificationChannels.NOTIFICATION_CHANNEL_CALLS_V4.name
        )

    fun getMessageRingtoneUri(context: Context, appPreferences: AppPreferences): Uri? =
        getRingtoneUri(
            context,
            appPreferences.messageRingtoneUri,
            DEFAULT_MESSAGE_RINGTONE_URI,
            NotificationChannels.NOTIFICATION_CHANNEL_MESSAGES_V4.name
        )

    fun loadAvatarSync(avatarUrl: String, context: Context): IconCompat? {
        var avatarIcon: IconCompat? = null

        val request = ImageRequest.Builder(context)
            .data(avatarUrl)
            .transformations(CircleCropTransformation())
            .placeholder(R.drawable.account_circle_96dp)
            .target(
                onSuccess = { result ->
                    val bitmap = (result as BitmapDrawable).bitmap
                    avatarIcon = IconCompat.createWithBitmap(bitmap)
                },
                onError = { error ->
                    error?.let {
                        val bitmap = (error as BitmapDrawable).bitmap
                        avatarIcon = IconCompat.createWithBitmap(bitmap)
                    }
                    Log.w(TAG, "Can't load avatar for URL: $avatarUrl")
                }
            )
            .build()

        context.imageLoader.executeBlocking(request)

        return avatarIcon
    }
    
    /**
     * Smart notification grouping like WhatsApp
     * Checks if we should play sound based on grouping preferences
     */
    fun shouldPlaySoundForMessage(context: Context, conversationToken: String): Boolean {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        
        // If smart grouping is disabled, always play sound
        if (!prefs.getBoolean(PREF_SMART_GROUPING_ENABLED, true)) {
            return true
        }
        
        // Get last notification time for this conversation
        val lastNotificationTime = getLastNotificationTime(context, conversationToken)
        val currentTime = System.currentTimeMillis()
        val groupingTimeout = prefs.getInt(PREF_GROUPING_TIMEOUT_MINUTES, DEFAULT_GROUPING_TIMEOUT_MINUTES)
        val timeoutMs = groupingTimeout * 60 * 1000L
        
        // If enough time has passed, play sound
        val shouldPlay = (currentTime - lastNotificationTime) > timeoutMs
        
        // Update last notification time
        updateLastNotificationTime(context, conversationToken, currentTime)
        
        Log.d(TAG, "Smart grouping for $conversationToken: shouldPlay=$shouldPlay, timeSince=${currentTime - lastNotificationTime}ms, timeout=${timeoutMs}ms")
        
        return shouldPlay
    }
    
    /**
     * Check if we should show notification as ongoing (has unread messages)
     */
    fun hasUnreadMessages(context: Context, conversationToken: String): Boolean {
        val prefs = context.getSharedPreferences("unread_messages", Context.MODE_PRIVATE)
        return prefs.getBoolean("${conversationToken}_has_unread", false)
    }
    
    /**
     * Mark conversation as having unread messages
     */
    fun markConversationAsUnread(context: Context, conversationToken: String) {
        val prefs = context.getSharedPreferences("unread_messages", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("${conversationToken}_has_unread", true)
            .putLong("${conversationToken}_unread_since", System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Mark conversation as read
     */
    fun markConversationAsRead(context: Context, conversationToken: String) {
        val prefs = context.getSharedPreferences("unread_messages", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("${conversationToken}_has_unread", false)
            .remove("${conversationToken}_unread_since")
            .apply()
    }
    
    private fun getLastNotificationTime(context: Context, conversationToken: String): Long {
        val prefs = context.getSharedPreferences("notification_timing", Context.MODE_PRIVATE)
        return prefs.getLong("${conversationToken}_last_notification", 0L)
    }
    
    private fun updateLastNotificationTime(context: Context, conversationToken: String, timestamp: Long) {
        val prefs = context.getSharedPreferences("notification_timing", Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("${conversationToken}_last_notification", timestamp)
            .apply()
    }

    private data class Channel(val id: String, val name: String, val description: String, val isImportant: Boolean)
}
