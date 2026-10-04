package ru.alliance.xiaoxi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderScheduler {

    private val times = listOf(
        11 to 0,
        12 to 30,
        14 to 0,
        15 to 30,
        17 to 0
    )

    fun scheduleAll(context: Context) {
        times.forEachIndexed { index, time ->
            scheduleNext(
                context = context,
                requestCode = index,
                hour = time.first,
                minute = time.second
            )
        }
    }

    fun scheduleNext(
        context: Context,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
    putExtra("requestCode", requestCode)
    putExtra("hour", hour)
    putExtra("minute", minute)
}

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }

            while (
                get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            ) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}
