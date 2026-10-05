package ru.alliance.xiaoxi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderScheduler {

    private val xiaoxiTimes = listOf(
        11 to 0,
        12 to 30,
        14 to 0,
        15 to 30,
        17 to 0
    )

    fun scheduleAll(context: Context) {

        // Сяоси
        xiaoxiTimes.indices.forEach { index ->
            scheduleNextXiaoxi(context, index)
        }

        // Сяоши — дневной сон в 13:30
        scheduleNextXiaoshi(context)

        // Цигун — 5 минут в 16:30
        scheduleNextQigong(context)
    }

    private fun getTime(context: Context, key: String, hour: Int, minute: Int): Pair<Int, Int> {
        val saved = context.getSharedPreferences("xiaoxi", Context.MODE_PRIVATE)
            .getInt(key, hour * 60 + minute)
        return (saved / 60) to (saved % 60)
    }

    fun getXiaoxiTime(context: Context, index: Int): Pair<Int, Int> {
        val default = xiaoxiTimes[index]
        return getTime(context, "time_xiaoxi_$index", default.first, default.second)
    }

    fun getXiaoshiTime(context: Context): Pair<Int, Int> =
        getTime(context, "time_xiaoshi", 13, 30)

    fun getQigongTime(context: Context): Pair<Int, Int> =
        getTime(context, "time_qigong", 16, 30)

    fun scheduleNextXiaoxi(context: Context, index: Int) {
        val time = getXiaoxiTime(context, index)
        scheduleNext(context, index, time.first, time.second)
    }

    fun scheduleSavedTime(context: Context, key: String) {
        when {
            key.startsWith("time_xiaoxi_") ->
                scheduleNextXiaoxi(context, key.removePrefix("time_xiaoxi_").toInt())
            key == "time_xiaoshi" -> scheduleNextXiaoshi(context)
            key == "time_qigong" -> scheduleNextQigong(context)
        }
    }

    fun scheduleNext(
        context: Context,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {

        val calendar = nextWeekdayTime(
            hour,
            minute
        )

        scheduleAlarmClock(
            context,
            requestCode,
            calendar.timeInMillis,
            hour,
            minute,
            "regular",
            0
        )
    }

    fun scheduleNextXiaoshi(context: Context) {

        val (hour, minute) = getXiaoshiTime(context)
        val calendar = nextWeekdayTime(hour, minute)

        scheduleAlarmClock(
            context,
            3001,
            calendar.timeInMillis,
            hour,
            minute,
            "xiaoshi",
            0
        )
    }

    fun scheduleNextQigong(context: Context) {

        val (hour, minute) = getQigongTime(context)
        val calendar = nextWeekdayTime(hour, minute)

        scheduleAlarmClock(
            context,
            4001,
            calendar.timeInMillis,
            hour,
            minute,
            "qigong",
            0
        )
    }

    // Тест Сяоси через 1 минуту
    fun scheduleTest(context: Context) {

        scheduleAlarmClock(
            context,
            -1,
            System.currentTimeMillis() + 1 * 60 * 1000L,
            0,
            0,
            "regular",
            0
        )
    }

    // Тест Сяоши через 1 минуту
    fun scheduleXiaoshiTest(context: Context) {

        scheduleAlarmClock(
            context,
            3004,
            System.currentTimeMillis() + 1 * 60 * 1000L,
            0,
            0,
            "xiaoshi_test",
            0
        )
    }

    // Тест Цигун через 1 минуту
    fun scheduleQigongTest(context: Context) {

        scheduleAlarmClock(
            context,
            4004,
            System.currentTimeMillis() + 1 * 60 * 1000L,
            0,
            0,
            "qigong_test",
            0
        )
    }

    // Таймер этапов Сяоси
    fun scheduleExerciseTimer(
        context: Context,
        minutes: Int,
        stage: Int
    ) {

        val requestCode =
            if (stage == 1) 1001 else 1002

        scheduleAlarmClock(
            context,
            requestCode,
            System.currentTimeMillis() +
                minutes * 60 * 1000L,
            0,
            0,
            "exercise",
            stage
        )
    }

    // Отложенная Сяоси
    fun scheduleSnooze(
        context: Context,
        minutes: Int
    ) {

        scheduleAlarmClock(
            context,
            2001,
            System.currentTimeMillis() +
                minutes * 60 * 1000L,
            0,
            0,
            "snooze",
            0
        )
    }

    // Таймер Сяоши
    fun scheduleXiaoshiTimer(
        context: Context,
        minutes: Int,
        isTest: Boolean = false
    ) {

        scheduleAlarmClock(
            context,
            3002,
            System.currentTimeMillis() +
                minutes * 60 * 1000L,
            0,
            0,
            if (isTest) "xiaoshi_timer_test" else "xiaoshi_timer",
            0
        )
    }

    // Отложенная Сяоши
    fun scheduleXiaoshiSnooze(
        context: Context,
        minutes: Int
    ) {

        scheduleAlarmClock(
            context,
            3003,
            System.currentTimeMillis() +
                minutes * 60 * 1000L,
            0,
            0,
            "xiaoshi_snooze",
            0
        )
    }

    // Таймер Цигун
    fun scheduleQigongTimer(
        context: Context,
        minutes: Int,
        isTest: Boolean = false
    ) {

        scheduleAlarmClock(
            context,
            4002,
            System.currentTimeMillis() +
                minutes * 60 * 1000L,
            0,
            0,
            if (isTest) "qigong_timer_test" else "qigong_timer",
            0
        )
    }

    // Отложенный Цигун
    fun scheduleQigongSnooze(
        context: Context,
        minutes: Int
    ) {

        scheduleAlarmClock(
            context,
            4003,
            System.currentTimeMillis() +
                minutes * 60 * 1000L,
            0,
            0,
            "qigong_snooze",
            0
        )
    }

    fun stopActivePractice(context: Context) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (code in listOf(1001, 1002, 3002, 4002)) {
            val intent = Intent(context, ReminderReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context, code, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                manager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun nextWeekdayTime(
        hour: Int,
        minute: Int
    ): Calendar {

        return Calendar.getInstance().apply {

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
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val receiverIntent =
            Intent(
                context,
                ReminderReceiver::class.java
            )

        receiverIntent.putExtra(
            "requestCode",
            requestCode
        )

        receiverIntent.putExtra(
            "hour",
            hour
        )

        receiverIntent.putExtra(
            "minute",
            minute
        )

        receiverIntent.putExtra(
            "alarmType",
            alarmType
        )

        receiverIntent.putExtra(
            "stage",
            stage
        )

        val receiverPendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val showIntent =
            Intent(
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
