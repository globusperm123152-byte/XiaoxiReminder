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

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", 0)
        val hour = intent.getIntExtra("hour", 11)
        val minute = intent.getIntExtra("minute", 0)

        // Новый ID специально: Android создаст совершенно новый канал.
        val channelId = "xiaoxi_alarm_v3"

        val soundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Сяоси — рабочие паузы",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Звуковые напоминания о рабочих паузах"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            manager.createNotificationChannel(channel)
        }

        val openAppIntent =
            Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Сяоси")
                .setContentText(
                    "Пора сделать паузу: 10 минут для глаз и 5 минут спокойно походить."
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 500, 300, 500))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        try {
            NotificationManagerCompat
                .from(context)
                .notify(
                    if (requestCode < 0) 999 else requestCode + 100,
                    notification
                )
        } catch (_: SecurityException) {
        }

        // Тестовое уведомление (-1) повторно не планируем.
        if (requestCode >= 0) {
            ReminderScheduler.scheduleNext(
                context,
                requestCode,
                hour,
                minute
            )
        }
    }
}
