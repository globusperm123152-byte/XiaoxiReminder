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
        private const val CHANNEL_ID = "xiaoxi_fullscreen_alarm_v3"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)

        val alarmType =
            intent.getStringExtra("alarmType") ?: "regular"

        val stage =
            intent.getIntExtra("stage", 0)

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
            putExtra("alarmType", alarmType)
            putExtra("stage", stage)
        }

        val fullScreenPendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                alarmActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notificationText =
            when {
                alarmType == "exercise" && stage == 1 ->
                    "10 минут закончились"

                alarmType == "exercise" && stage == 2 ->
                    "5 минут ходьбы закончились"

                alarmType == "snooze" ->
                    "Отложенное напоминание"

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
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(
                    fullScreenPendingIntent,
                    true
                )
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

        // Только обычное рабочее напоминание
        // назначаем на следующий рабочий день.
        if (
            alarmType == "regular" &&
            requestCode >= 0
        )
