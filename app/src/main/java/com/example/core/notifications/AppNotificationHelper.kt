package com.example.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object AppNotificationHelper {
    private const val TAG = "AppNotificationHelper"
    const val CHANNEL_ID_SOUND = "real_reward_alerts_sound_v1"
    const val CHANNEL_ID_SILENT = "real_reward_alerts_silent_v1"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        if (manager.getNotificationChannel(CHANNEL_ID_SOUND) == null) {
            val soundChannel = NotificationChannel(
                CHANNEL_ID_SOUND,
                "REAL REWARD Alerts & Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Task approvals, withdrawals, VIP updates, and app announcements with sound"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                enableLights(true)
            }
            manager.createNotificationChannel(soundChannel)
        }

        if (manager.getNotificationChannel(CHANNEL_ID_SILENT) == null) {
            val silentChannel = NotificationChannel(
                CHANNEL_ID_SILENT,
                "REAL REWARD Silent Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Muted alerts without notification ringtone"
                setSound(null, null)
                enableVibration(false)
            }
            manager.createNotificationChannel(silentChannel)
        }
    }

    fun playNotificationSound(context: Context) {
        val played = runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                if (ringtone != null) {
                    ringtone.play()
                    true
                } else false
            } else false
        }.getOrDefault(false)

        if (!played) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
                tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
            }.onFailure {
                Log.w(TAG, "Unable to play notification tone: ${it.message}")
            }
        }
    }

    fun showSystemNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        withSound: Boolean
    ) {
        ensureChannels(context)

        if (!hasNotificationPermission(context)) {
            if (withSound) playNotificationSound(context)
            return
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (withSound) CHANNEL_ID_SOUND else CHANNEL_ID_SILENT
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (withSound) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (withSound) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (soundUri != null) {
                builder.setSound(soundUri)
            } else {
                builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            }
        } else {
            builder.setSilent(true)
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            if (withSound) {
                playNotificationSound(context)
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${se.message}")
        }
    }

    fun openDownloadOrUpdateUrl(context: Context, rawUrl: String): Boolean {
        val clean = rawUrl.trim()
        if (!clean.startsWith("https://", ignoreCase = true) && !clean.startsWith("http://", ignoreCase = true)) {
            return false
        }
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun shareAppDownload(context: Context, url: String?, brandName: String, latestVersion: String) {
        val cleanUrl = url?.trim()?.takeIf { it.startsWith("http") }
        val shareBody = buildString {
            append("$brandName (v$latestVersion) — WORK • EARN • REWARD")
            if (cleanUrl != null) {
                append("\nDownload: $cleanUrl")
            }
        }
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, brandName)
                putExtra(Intent.EXTRA_TEXT, shareBody)
            }
            val chooser = Intent.createChooser(sendIntent, brandName).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }
}
