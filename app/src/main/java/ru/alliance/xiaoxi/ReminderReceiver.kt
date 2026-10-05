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
        private const val FINISH_CHANNEL_ID = "xiaoxi_finish_alarm_v5"
    }

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)
        val alarmType = intent.getStringExtra("alarmType") ?: "regular"
        val stage = intent.getIntExtra("stage", 0)

        val notificationId =
            if (requestCode < 0) 999 else 100 + requestCode

        createChannel(context)

        val activityIntent =
            Intent(context, AlarmActivity::class.java)

        activityIntent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP

        activityIntent.putExtra("notificationId", notificationId)
        activityIntent.putExtra("alarmType", alarmType)
        activityIntent.putExtra("stage", stage)

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

            alarmType == "xiaoshi_test" ->
                "Тест дневного сна — 2 минуты"

            alarmType == "xiaoshi_timer_test" ->
                "Тест сна завершён — 2 минуты"

            alarmType == "xiaoshi" ->
                "Время дневного сна"

            alarmType == "xiaoshi_timer" ->
                "25 минут сна закончились"

            alarmType == "xiaoshi_snooze" ->
                "Пора на отложенный дневной сон"

            alarmType == "qigong_test" ->
                "Тест Цигун — 2 минуты"

            alarmType == "qigong_timer_test" ->
                "Тест Цигун завершён — 2 минуты"

            alarmType == "qigong" ->
                "Время сделать Цигун"

            alarmType == "qigong_timer" ->
                "5 минут Цигун закончились"

            alarmType == "qigong_snooze" ->
                "Пора сделать отложенный Цигун"

            else ->
                "Пора сделать рабочую паузу"
        }

        val title =
            if (
                alarmType.startsWith("qigong") ||
                false
            ) {
                "Цигун"
            } else {
                "Сяоси"
            }

        val isCompletion = alarmType == "xiaoshi_timer" ||
            alarmType == "xiaoshi_timer_test" ||
            alarmType == "qigong_timer" ||
            alarmType == "qigong_timer_test" ||
            alarmType == "exercise"

        val notification =
            NotificationCompat.Builder(
                context,
                if (isCompletion) FINISH_CHANNEL_ID else CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_lock_idle_alarm
                )
                .setContentTitle(title)
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

        // Следующая Сяоси
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

        // Следующая Сяоши
        if (alarmType == "xiaoshi") {
            ReminderScheduler.scheduleNextXiaoshi(
                context
            )
        }

        // Следующий Цигун
        if (alarmType == "qigong") {
            ReminderScheduler.scheduleNextQigong(
                context
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

        manager.createNotificationChannel(channel)

        // Separate alarm channel: HONOR may suppress full-screen
        // presentation for a timer completion, so it must also
        // sound and vibrate without opening the notification.
        val finishChannel = NotificationChannel(
            FINISH_CHANNEL_ID,
            "Завершение практики — будильник",
            NotificationManager.IMPORTANCE_HIGH
        )
        finishChannel.setSound(
            android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_ALARM
            ),
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(
                    android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()
        )
        finishChannel.enableVibration(true)
        finishChannel.lockscreenVisibility =
            android.app.Notification.VISIBILITY_PUBLIC
        manager.createNotificationChannel(finishChannel)
    }
}
