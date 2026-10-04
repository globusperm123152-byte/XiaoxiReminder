package ru.alliance.xiaoxi

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 100, 60, 60)
        }

        val title = TextView(this).apply {
            text = "Сяоси"
            textSize = 34f
        }

        val info = TextView(this).apply {
            text = """
                
                Рабочие паузы
                
                Пн–Пт
                
                11:00
                12:30
                14:00
                15:30
                17:00
                
                10 минут — закрыть глаза
                5 минут — спокойно походить
            """.trimIndent()
            textSize = 19f
        }

        val button = Button(this).apply {
            text = "ВКЛЮЧИТЬ НАПОМИНАНИЯ"
            setOnClickListener {
                enableExactAlarmsIfNeeded()
ReminderScheduler.scheduleAll(this@MainActivity)
                text = "НАПОМИНАНИЯ ВКЛЮЧЕНЫ ✓"
            }
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(button)

        setContentView(layout)
    }

    private fun enableExactAlarmsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)

            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                )
            }
        }
    }

    private fun scheduleReminders() {
        val times = listOf(
            11 to 0,
            12 to 30,
            14 to 0,
            15 to 30,
            17 to 0
        )

        times.forEachIndexed { index, time ->
            scheduleNext(index, time.first, time.second)
        }
    }

    private fun scheduleNext(
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val alarmManager =
            getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("requestCode", requestCode)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            while (
                timeInMillis <= System.currentTimeMillis() ||
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
