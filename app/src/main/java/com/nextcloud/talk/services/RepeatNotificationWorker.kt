/*
 * Nextcloud Talk - Android Client
 * Repeat Notifications Service - like WhatsApp
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH and Nextcloud contributors  
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.services

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import autodagger.AutoInjector
import com.nextcloud.talk.R
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.utils.NotificationUtils
import com.nextcloud.talk.utils.preferences.AppPreferences
import javax.inject.Inject

/**
 * Worker for repeating notifications for unread messages (like WhatsApp)
 * This service checks for unread messages and sends reminder notifications
 */
@AutoInjector(NextcloudTalkApplication::class)
class RepeatNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    @Inject
    lateinit var appPreferences: AppPreferences

    companion object {
        private const val TAG = "RepeatNotificationWorker"
        const val REPEAT_INTERVAL_MINUTES = 5 // Repeat every 5 minutes for unread
        const val MAX_REPEATS = 3 // Maximum 3 repeats
    }

    override fun doWork(): Result {
        NextcloudTalkApplication.sharedApplication?.componentApplication?.inject(this)
        
        Log.d(TAG, "Checking for unread messages to repeat notifications")
        
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (!prefs.getBoolean(NotificationUtils.PREF_REPEAT_NOTIFICATIONS, true)) {
            Log.d(TAG, "Repeat notifications disabled")
            return Result.success()
        }
        
        checkAndRepeatUnreadNotifications()
        
        return Result.success()
    }
    
    private fun checkAndRepeatUnreadNotifications() {
        val unreadPrefs = applicationContext.getSharedPreferences("unread_messages", Context.MODE_PRIVATE)
        val allUnread = unreadPrefs.all
        
        val currentTime = System.currentTimeMillis()
        val repeatInterval = REPEAT_INTERVAL_MINUTES * 60 * 1000L
        
        for ((key, value) in allUnread) {
            if (key.endsWith("_has_unread") && value == true) {
                val conversationToken = key.removeSuffix("_has_unread")
                val unreadSince = unreadPrefs.getLong("${conversationToken}_unread_since", 0L)
                val repeatCount = unreadPrefs.getInt("${conversationToken}_repeat_count", 0)
                
                // Check if enough time has passed and we haven't exceeded max repeats
                if (currentTime - unreadSince > repeatInterval && 
                    repeatCount < MAX_REPEATS && 
                    unreadSince > 0) {
                    
                    sendRepeatNotification(conversationToken, repeatCount + 1)
                    
                    // Update repeat count
                    unreadPrefs.edit()
                        .putInt("${conversationToken}_repeat_count", repeatCount + 1)
                        .apply()
                }
            }
        }
    }
    
    private fun sendRepeatNotification(conversationToken: String, repeatCount: Int) {
        Log.d(TAG, "Sending repeat notification #$repeatCount for conversation: $conversationToken")
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create subtle repeat notification
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationUtils.NotificationChannels.NOTIFICATION_CHANNEL_MESSAGES_V4.name
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("💬 Непрочитанные сообщения")
            .setContentText("У вас есть непрочитанные сообщения в Nextcloud Talk")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Lower priority for repeats
            .setAutoCancel(true)
            .setSound(getSubtleNotificationSound())
            .setOnlyAlertOnce(false)
            .build()
        
        // Use unique ID for repeat notifications
        val notificationId = "repeat_$conversationToken".hashCode()
        notificationManager.notify(notificationId, notification)
    }
    
    private fun getSubtleNotificationSound(): Uri? {
        // Use a more subtle sound for repeat notifications
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }
}
