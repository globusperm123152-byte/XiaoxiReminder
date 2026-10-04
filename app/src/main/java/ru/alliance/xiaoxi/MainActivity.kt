package ru.alliance.xiaoxi

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
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

    private lateinit var enableButton: Button
    private lateinit var testButton: Button
    private lateinit var xiaoshiTestButton: Button
    private lateinit var qigongTestButton: Button

    private var waitingForExactAlarmPermission = false
    private var waitingForFullScreenPermission = false

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 100
        private const val PREFS_NAME = "xiaoxi"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createInterface()
        updateInterface()
    }

    private fun createInterface() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "Сяоси"
            textSize = 34f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val info = TextView(this).apply {
            text = """
                
                РАБОЧИЕ ПАУЗЫ — СЯОСИ
                
                Понедельник — пятница
                
                11:00
                12:30
                14:00
                15:30
                17:00
                
                10 минут — закрыть глаза
                5 минут — спокойно походить
                
                СЯОШИ — ДНЕВНОЙ СОН
                
                13:30
                
                25 минут отдыха
                
                ЦИГУН
                
                16:30
                
                5 минут — дыхание и плавные движения
            """.trimIndent()

            textSize = 19f
            setTextColor(Color.DKGRAY)
        }

        enableButton = Button(this).apply {
            textSize = 17f
            setOnClickListener {
                enableReminders()
            }
        }

        testButton = Button(this).apply {
            text = "ТЕСТ СЯОСИ — ЧЕРЕЗ 2 МИНУТЫ"
            textSize = 15f
            setOnClickListener {
                startTest()
            }
        }

        xiaoshiTestButton = Button(this).apply {
            text = "ТЕСТ СЯОШИ — ЧЕРЕЗ 2 МИНУТЫ"
            textSize = 15f
            setOnClickListener {
                startXiaoshiTest()
            }
        }

        qigongTestButton = Button(this).apply {
            text = "ТЕСТ ЦИГУН — ЧЕРЕЗ 2 МИНУТЫ"
            textSize = 15f
            setOnClickListener {
                startQigongTest()
            }
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(enableButton)
        layout.addView(testButton)
        layout.addView(xiaoshiTestButton)
        layout.addView(qigongTestButton)

        setContentView(layout)
    }

    private fun updateInterface() {

        val enabled =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            ).getBoolean(
                KEY_REMINDERS_ENABLED,
                false
            )

        if (enabled) {
            enableButton.text = "НАПОМИНАНИЯ ВКЛЮЧЕНЫ ✓"
            enableButton.isEnabled = false
        } else {
            enableButton.text = "ВКЛЮЧИТЬ НАПОМИНАНИЯ"
            enableButton.isEnabled = true
        }
    }

    private fun enableReminders() {

        if (!hasNotificationPermission()) {
            requestNotificationPermission()
            return
        }

        if (!hasExactAlarmPermission()) {
            requestExactAlarmPermission()
            return
        }

        if (!hasFullScreenPermission()) {
            requestFullScreenPermission()
            return
        }

        ReminderScheduler.scheduleAll(this)

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                KEY_REMINDERS_ENABLED,
                true
            )
            .apply()

        updateInterface()
    }

    private fun startTest() {

        if (!permissionsReady()) return

        ReminderScheduler.scheduleTest(this)

        testButton.text = "ТЕСТ СЯОСИ ЗАПУЩЕН ✓"
        testButton.isEnabled = false
    }

    private fun startXiaoshiTest() {

        if (!permissionsReady()) return

        ReminderScheduler.scheduleXiaoshiTest(this)

        xiaoshiTestButton.text = "ТЕСТ СЯОШИ ЗАПУЩЕН ✓"
        xiaoshiTestButton.isEnabled = false
    }

    private fun startQigongTest() {

        if (!permissionsReady()) return

        ReminderScheduler.scheduleQigongTest(this)

        qigongTestButton.text = "ТЕСТ ЦИГУН ЗАПУЩЕН ✓"
        qigongTestButton.isEnabled = false
    }

    private fun permissionsReady(): Boolean {

        if (!hasNotificationPermission()) {
            requestNotificationPermission()
            return false
        }

        if (!hasExactAlarmPermission()) {
            requestExactAlarmPermission()
            return false
        }

        if (!hasFullScreenPermission()) {
            requestFullScreenPermission()
            return false
        }

        return true
    }

    private fun hasNotificationPermission(): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST
            )
        }
    }

    private fun hasExactAlarmPermission(): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        return getSystemService(
            AlarmManager::class.java
        ).canScheduleExactAlarms()
    }

    private fun requestExactAlarmPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            waitingForExactAlarmPermission = true

            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun hasFullScreenPermission(): Boolean {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true
        }

        return getSystemService(
            NotificationManager::class.java
        ).canUseFullScreenIntent()
    }

    private fun requestFullScreenPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {

            waitingForFullScreenPermission = true

            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    Uri.parse("package:$packageName")
                )
            )
        }
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
            requestCode == NOTIFICATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            enableReminders()
        }
    }

    override fun onResume() {
        super.onResume()

        updateInterface()

        if (
            waitingForExactAlarmPermission &&
            hasExactAlarmPermission()
        ) {
            waitingForExactAlarmPermission = false
            enableReminders()
            return
        }

        if (
            waitingForFullScreenPermission &&
            hasFullScreenPermission()
        ) {
            waitingForFullScreenPermission = false
            enableReminders()
        }
    }
}
