package ru.alliance.xiaoxi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val enabled = context
            .getSharedPreferences("xiaoxi", Context.MODE_PRIVATE)
            .getBoolean("reminders_enabled", false)

        if (enabled) {
            ReminderScheduler.scheduleAll(context)
        }
    }
}
