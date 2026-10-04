package ru.alliance.xiaoxi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", 0)
        val hour = intent.getIntExtra("hour", 11)
        val minute = intent.getIntExtra("minute", 0)

        val channelId = "xiaoxi_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Напоминания Сяоси",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о рабочих паузах"
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Сяоси")
            .setContentText("Пора сделать паузу: 10 минут для глаз и 5 минут спокойно походить.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                requestCode + 100,
                notification
            )
        } catch (_: SecurityException) {
        }

        ReminderScheduler.scheduleNext(
            context,
            requestCode,
            hour,
            minute
        )
    }
}
