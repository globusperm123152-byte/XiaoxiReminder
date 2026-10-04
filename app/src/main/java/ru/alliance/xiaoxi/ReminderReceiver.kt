package ru.alliance.xiaoxi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val requestCode = intent.getIntExtra("requestCode", -1)
        val hour = intent.getIntExtra("hour", 0)
        val minute = intent.getIntExtra("minute", 0)

        // Запускаем полноэкранный сигнал Сяоси
        val alarmIntent = Intent(
            context,
            AlarmActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(alarmIntent)

        // Обычные рабочие напоминания ставим снова
        // на следующий рабочий день.
        // Тестовое напоминание (-1) не повторяем.
        if (requestCode >= 0) {
            ReminderScheduler.scheduleNext(
                context = context,
                requestCode = requestCode,
                hour = hour,
                minute = minute
            )
        }
    }
}
