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
        private const val CHANNEL_ID = "xiaoxi_fullscreen_alarm_v1"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)

        createAlarmChannel(context)

        // Экран, который должен открыться как будильник
        val alarmActivityIntent = Intent(
            context,
            AlarmActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            if (requestCode < 0) 999 else requestCode,
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
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                if (requestCode < 0) 999 else 100 + requestCode,
                notification
            )
        } catch (_: SecurityException) {
        }

        // Тест (-1) выполняется только один раз.
        // Обычное напоминание назначаем на следующий рабочий день.
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
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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

            setSound(alarmSound, audioAttributes)
        }

        val manager = context.getSystemService(
            NotificationManager::class.java
        )

        manager.createNotificationChannel(channel)
    }
}
