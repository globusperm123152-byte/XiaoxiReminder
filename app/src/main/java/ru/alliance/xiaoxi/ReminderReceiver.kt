package ru.alliance.xiaoxi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "xiaoxi_fullscreen_alarm_v2"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)

        val notificationId =
            if (requestCode < 0) 999 else 100 + requestCode

        createAlarmChannel(context)

        val alarmActivityIntent = Intent(
            context,
            AlarmActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP

            putExtra("notificationId", notificationId)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Сяоси")
            .setContentText("Пора сделать рабочую паузу")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat
                .from(context)
                .notify(
                    notificationId,
                    notification
                )
        } catch (_: SecurityException) {
        }

        if (requestCode >= 0) {
            ReminderScheduler.scheduleNext(
                context = context,
                requestCode = requestCode,
                hour = hour,
                minute = minute
            )
        }
    }

    private fun createAlarmChannel(context: Context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val alarmSound = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_ALARM
        )

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(
                AudioAttributes.CONTENT_TYPE_SONIFICATION
            )
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Сяоси — будильник",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Полноэкранные напоминания Сяоси"

            enableVibration(true)

            vibrationPattern = longArrayOf(
                0, 700, 400, 700, 400
            )

            setSound(
                alarmSound,
                audioAttributes
            )

            lockscreenVisibility =
                android.app.Notification.VISIBILITY_PUBLIC
        }

        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
