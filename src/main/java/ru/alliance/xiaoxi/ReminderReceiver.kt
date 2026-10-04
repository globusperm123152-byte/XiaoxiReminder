package ru.alliance.xiaoxi

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        showNotification(context)

        val requestCode = intent.getIntExtra("requestCode", 0)
        val hour = intent.getIntExtra("hour", 11)
        val minute = intent.getIntExtra("minute", 0)

        scheduleNextWeekday(
            context,
            requestCode,
            hour,
            minute
        )
    }

    private fun showNotification(context: Context) {
        val channelId = "xiaoxi_reminders"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Напоминания Сяоси",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о рабочих паузах"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(channel)
        }

        val openApp = Intent(context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            500,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            channelId
        )
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Сяоси — время сделать паузу")
            .setContentText(
                "10 минут закрой глаза, затем 5 минут спокойно походи."
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Пора отвлечься от работы. " +
                    "10 минут отдохни с закрытыми глазами, " +
                    "затем 5 минут спокойно походи."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }

    private fun scheduleNextWeekday(
        context: Context,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)

            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            while (
                get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            ) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {
            putExtra("requestCode", requestCode)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE)
                    as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}
