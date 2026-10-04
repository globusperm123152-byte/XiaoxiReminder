package ru.alliance.xiaoxi

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private var waitingForExactAlarmPermission = false
    private lateinit var enableButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 60)
        }

        val title = TextView(this).apply {
            text = "Сяоси"
            textSize = 34f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val info = TextView(this).apply {
            text = """
                
                Рабочие паузы
                
                Понедельник — пятница
                
                11:00
                12:30
                14:00
                15:30
                17:00
                
                10 минут — закрыть глаза
                5 минут — спокойно походить
            """.trimIndent()

            textSize = 20f
            setTextColor(Color.DKGRAY)
        }

        enableButton = Button(this).apply {
            text = "ВКЛЮЧИТЬ НАПОМИНАНИЯ"
            textSize = 17f

            setOnClickListener {
                enableReminders()
            }
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(enableButton)

        setContentView(layout)
    }

    private fun enableReminders() {

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
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
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)

            if (!alarmManager.canScheduleExactAlarms()) {
                waitingForExactAlarmPermission = true

                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )

                startActivity(intent)
                return
            }
        }

        startReminders()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableReminders()
        }
    }

    override fun onResume() {
        super.onResume()

        if (waitingForExactAlarmPermission) {
            val alarmManager = getSystemService(AlarmManager::class.java)

            if (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            ) {
                waitingForExactAlarmPermission = false
                startReminders()
            }
        }
    }

    private fun startReminders() {
        ReminderScheduler.scheduleAll(this)

        getSharedPreferences("xiaoxi", MODE_PRIVATE)
            .edit()
            .putBoolean("reminders_enabled", true)
            .apply()

        enableButton.text = "НАПОМИНАНИЯ ВКЛЮЧЕНЫ ✓"
        enableButton.isEnabled = false
    }
}
