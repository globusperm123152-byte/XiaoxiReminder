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

    companion object {
        private const val CHANNEL_ID = "xiaoxi_alarm_v4"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)
        val alarmType = intent.getStringExtra("alarmType") ?: "regular"
        val stage = intent.getIntExtra("stage", 0)

        val notificationId = if (requestCode < 0) {
            999
        } else {
            100 + requestCode
        }

        createChannel(context)

        val activityIntent = Intent(
            context,
            AlarmActivity::class.java
        )

        activityIntent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP

        activityIntent.putExtra(
            "notificationId",
            notificationId
        )

        activityIntent.putExtra(
            "alarmType",
            alarmType
        )

        activityIntent.putExtra(
            "stage",
            stage
        )

        val activityPendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val text = when {
            alarmType == "exercise" && stage == 1 ->
                "10 минут закончились"

            alarmType == "exercise" && stage == 2 ->
                "5 минут ходьбы закончились"

            alarmType == "snooze" ->
                "Пора сделать отложенную паузу"

            else ->
                "Пора сделать рабочую паузу"
        }

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_lock_idle_alarm
                )
                .setContentTitle("Сяоси")
                .setContentText(text)
                .setPriority(
                    NotificationCompat.PRIORITY_MAX
                )
                .setCategory(
                    NotificationCompat.CATEGORY_ALARM
                )
                .setVisibility(
                    NotificationCompat.VISIBILITY_PUBLIC
                )
                .setFullScreenIntent(
                    activityPendingIntent,
                    true
                )
                .setContentIntent(
                    activityPendingIntent
                )
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

        if (
            alarmType == "regular" &&
            requestCode >= 0
        ) {
            ReminderScheduler.scheduleNext(
                context,
                requestCode,
                hour,
                minute
            )
        }
    }

    private fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT < 26) {
            return
        }

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Сяоси — будильник",
                NotificationManager.IMPORTANCE_HIGH
            )

        channel.description =
            "Напоминания и таймеры Сяоси"

        channel.enableVibration(true)

        channel.lockscreenVisibility =
            android.app.Notification.VISIBILITY_PUBLIC

        manager.createNotificationChannel(
            channel
        )
    }
}
