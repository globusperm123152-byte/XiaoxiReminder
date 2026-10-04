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

        scheduleAlarmClock(
            context = context,
            requestCode = requestCode,
            triggerTime = calendar.timeInMillis,
            hour = hour,
            minute = minute,
            alarmType = "regular",
            stage = 0
        )
    }

    fun scheduleTest(context: Context) {

        scheduleAlarmClock(
            context = context,
            requestCode = -1,
            triggerTime = System.currentTimeMillis() + 2 * 60 * 1000,
            hour = 0,
            minute = 0,
            alarmType = "regular",
            stage = 0
        )
    }

    fun scheduleExerciseTimer(
        context: Context,
        minutes: Int,
        stage: Int
    ) {

        scheduleAlarmClock(
            context = context,
            requestCode = if (stage == 1) 1001 else 1002,
            triggerTime = System.currentTimeMillis() +
                    minutes * 60 * 1000L,
            hour = 0,
            minute = 0,
            alarmType = "exercise",
            stage = stage
        )
    }

    fun scheduleSnooze(
        context: Context,
        minutes: Int
    ) {

        scheduleAlarmClock(
            context = context,
            requestCode = 2001,
            triggerTime = System.currentTimeMillis() +
                    minutes * 60 * 1000L,
            hour = 0,
            minute = 0,
            alarmType = "snooze",
            stage = 0
        )
    }

    private fun scheduleAlarmClock(
        context: Context,
        requestCode: Int,
        triggerTime: Long,
        hour: Int,
        minute: Int,
        alarmType: String,
        stage: Int
    ) {

        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE)
                    as AlarmManager

        val receiverIntent = Intent(
            context,
            ReminderReceiver::class.java
        ).apply {

            putExtra("requestCode", requestCode)
            putExtra("hour", hour)
            putExtra("minute", minute)

            putExtra("alarmType", alarmType)
            putExtra("stage", stage)
        }

        val receiverPendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val showIntent = Intent(
            context,
            MainActivity::class.java
        )

        val showPendingIntent =
            PendingIntent.getActivity(
                context,
                5000 + requestCode,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val alarmInfo =
            AlarmManager.AlarmClockInfo(
                triggerTime,
                showPendingIntent
            )

        alarmManager.setAlarmClock(
            alarmInfo,
            receiverPendingIntent
        )
    }
}
