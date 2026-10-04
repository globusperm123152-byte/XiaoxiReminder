package ru.alliance.xiaoxi

import android.app.Notification
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
        private const val CHANNEL_ID = "xiaoxi_alarm_v4"
        private const val TEST_NOTIFICATION_ID = 999
    }

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)

        createNotificationChannel(context)

        val openAppIntent = Intent(
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmSound = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_ALARM
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Сяоси")
            .setContentText(
                "Пора сделать паузу: 10 минут для глаз и 5 минут спокойно походить."
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 700, 300, 700))
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            val notificationId =
                if (requestCode < 0) {
                    TEST_NOTIFICATION_ID
                } else {
                    100 + requestCode
                }

            NotificationManagerCompat
                .from(context)
                .notify(notificationId, notification)

        } catch (_: SecurityException) {
        }

        // Обычное рабочее напоминание планируем
        // заново на следующий рабочий день.
        // Тестовое (-1) больше не повторяем.
        if (requestCode >= 0) {
            ReminderScheduler.scheduleNext(
                context = context,
                requestCode = requestCode,
                hour = hour,
                minute = minute
            )
        }
    }

    private fun createNotificationChannel(context: Context) {

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
            "Сяоси — рабочие паузы",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {

            description =
                "Звуковые напоминания о рабочих паузах"

            enableVibration(true)

            vibrationPattern =
                longArrayOf(0, 700, 300, 700)

            setSound(
                alarmSound,
                audioAttributes
            )

            lockscreenVisibility =
                Notification.VISIBILITY_PUBLIC
        }

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(channel)
    }
}
