package ru.alliance.xiaoxi

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var enableButton: Button
    private lateinit var countdownLabel: TextView
    private val countdownHandler = Handler(Looper.getMainLooper())
    private val countdownTick = object : Runnable {
        override fun run() {
            updateCountdown()
            countdownHandler.postDelayed(this, 1000L)
        }
    }
    private val timeButtons = mutableMapOf<String, Button>()
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
        // Apply newly added schedules once after an app update,
        // without resetting in-progress practice timers.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_REMINDERS_ENABLED, false) &&
            prefs.getInt("schedule_version", 0) < 2) {
            ReminderScheduler.scheduleAll(this)
            prefs.edit().putInt("schedule_version", 2).apply()
        }
    }

    private fun rounded(color: Int, radius: Float = 28f): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = radius
            setColor(color)
        }

    private fun createInterface() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 38, 36, 48)
            setBackgroundColor(Color.rgb(245, 248, 253))
        }
        fun card(color: Int): LinearLayout {
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(28, 24, 28, 26)
                background = rounded(color)
                elevation = 5f
            }
            page.addView(content, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 22 })
            return content
        }
        fun text(parent: LinearLayout, value: String, size: Float, color: Int, bold: Boolean = false) {
            parent.addView(TextView(this).apply {
                this.text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(null, Typeface.BOLD)
                setPadding(0, 5, 0, 9)
            })
        }
        fun timeRow(parent: LinearLayout, key: String, hour: Int, minute: Int) {
            val button = Button(this).apply {
                textSize = 20f
                isAllCaps = false
                setTextColor(Color.rgb(30, 53, 82))
                backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                text = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute)
                setOnClickListener {
                    val current = readTime(key, hour, minute)
                    TimePickerDialog(
                        this@MainActivity,
                        { _, h, min ->
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit().putInt(key, h * 60 + min).apply()
                            updateTimeButtons()
                            if (getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                    .getBoolean(KEY_REMINDERS_ENABLED, false)) {
                                ReminderScheduler.scheduleSavedTime(this@MainActivity, key)
                            }
                        },
                        current.first, current.second, true
                    ).show()
                }
            }
            timeButtons[key] = button
            parent.addView(button, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 4 })
        }
        text(page, "Сяоси", 36f, Color.rgb(33, 57, 92), true)
        text(page, "Время для себя в течение дня", 17f, Color.rgb(105, 118, 139))

        val active = card(Color.rgb(222, 237, 252))
        text(active, "СЕЙЧАС", 14f, Color.rgb(49, 99, 163), true)
        countdownLabel = TextView(this).apply {
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(31, 75, 128))
        }
        active.addView(countdownLabel)

        val xiaoxi = card(Color.rgb(224, 239, 253))
        text(xiaoxi, "☀  Сяоси · Рабочие паузы", 22f, Color.rgb(30, 85, 150), true)
        text(xiaoxi, "Пн–пт · 10 минут отдыха + 5 минут ходьбы", 15f, Color.rgb(66, 94, 129))
        text(xiaoxi, "Нажми на время, чтобы изменить", 14f, Color.rgb(66, 94, 129))
        listOf(11 to 0, 12 to 30, 14 to 0, 15 to 30, 17 to 0)
            .forEachIndexed { index, pair ->
                timeRow(xiaoxi, "time_xiaoxi_$index", pair.first, pair.second)
            }

        val xiaoshi = card(Color.rgb(233, 226, 250))
        text(xiaoshi, "☾  Сяоши · Дневной сон", 22f, Color.rgb(97, 64, 152), true)
        text(xiaoshi, "Пн–пт · 25 минут спокойного отдыха", 15f, Color.rgb(103, 81, 134))
        timeRow(xiaoshi, "time_xiaoshi", 13, 30)

        val qigong = card(Color.rgb(220, 242, 230))
        text(qigong, "✦  Цигун · Движение", 22f, Color.rgb(34, 114, 82), true)
        text(qigong, "Пн–пт · 5 минут дыхания и плавных движений", 15f, Color.rgb(65, 111, 91))
        timeRow(qigong, "time_qigong", 16, 30)

        val actions = card(Color.rgb(238, 241, 246))
        text(actions, "НАСТРОЙКИ И ПРОВЕРКА", 17f, Color.rgb(52, 67, 89), true)
        enableButton = Button(this).apply {
            textSize = 16f
            setOnClickListener { enableReminders() }
        }
        testButton = Button(this).apply {
            textSize = 14f
            setOnClickListener { startTest() }
        }
        xiaoshiTestButton = Button(this).apply {
            textSize = 14f
            setOnClickListener { startXiaoshiTest() }
        }
        qigongTestButton = Button(this).apply {
            textSize = 14f
            setOnClickListener { startQigongTest() }
        }
        listOf(enableButton, testButton, xiaoshiTestButton, qigongTestButton)
            .forEach { actions.addView(it) }

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(page)
        })
        updateCountdown()
    }

    private fun updateCountdown() {
        if (!::countdownLabel.isInitialized) return
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val entries = listOf(
            Triple("Сяоси · глаза закрыты", "countdown_xiaoxi_eyes", 0),
            Triple("Сяоси · ходьба", "countdown_xiaoxi_walk", 0),
            Triple("Сяоши · отдых", "countdown_xiaoshi", 0),
            Triple("Цигун · дыхание", "countdown_qigong", 0)
        )
        val current = entries.map { it.first to prefs.getLong(it.second, 0L) }
            .filter { it.second > now }
            .minByOrNull { it.second }
        countdownLabel.text = if (current == null) {
            "Всё спокойно ☺"
        } else {
            val seconds = ((current.second - now + 999L) / 1000L).coerceAtLeast(0L)
            val minutes = seconds / 60
            val secs = seconds % 60
            current.first + "\n" + String.format(
                java.util.Locale.getDefault(), "%02d:%02d", minutes, secs
            )
        }
    }

    private fun readTime(key: String, defaultHour: Int, defaultMinute: Int): Pair<Int, Int> {
        val minutes = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(key, defaultHour * 60 + defaultMinute)
        return (minutes / 60) to (minutes % 60)
    }

    private fun updateTimeButtons() {
        val defaults = mapOf(
            "time_xiaoxi_0" to (11 to 0),
            "time_xiaoxi_1" to (12 to 30),
            "time_xiaoxi_2" to (14 to 0),
            "time_xiaoxi_3" to (15 to 30),
            "time_xiaoxi_4" to (17 to 0),
            "time_xiaoshi" to (13 to 30),
            "time_qigong" to (16 to 30)
        )
        timeButtons.forEach { (key, button) ->
            val default = defaults.getValue(key)
            val time = readTime(key, default.first, default.second)
            button.text = String.format(
                java.util.Locale.getDefault(), "%02d:%02d", time.first, time.second
            )
        }
    }

    private fun updateInterface() {
        updateTimeButtons()

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
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit().putInt("schedule_version", 2).apply()

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

        markTestStarted("xiaoxi_test_until")
    }

    private fun startXiaoshiTest() {

        if (!permissionsReady()) return

        ReminderScheduler.scheduleXiaoshiTest(this)

        markTestStarted("xiaoshi_test_until")
    }

    private fun startQigongTest() {

        if (!permissionsReady()) return

        ReminderScheduler.scheduleQigongTest(this)

        markTestStarted("qigong_test_until")
    }

    private fun refreshTestButtons() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val buttons = listOf(
            Triple(testButton, "xiaoxi_test_until", "ТЕСТ СЯОСИ — ЧЕРЕЗ 1 МИНУТУ"),
            Triple(xiaoshiTestButton, "xiaoshi_test_until", "ТЕСТ СЯОШИ — ЧЕРЕЗ 1 МИНУТУ"),
            Triple(qigongTestButton, "qigong_test_until", "ТЕСТ ЦИГУН — ЧЕРЕЗ 1 МИНУТУ")
        )
        buttons.forEach { (button, key, label) ->
            val pending = prefs.getLong(key, 0L) > now
            button.isEnabled = !pending
            button.text = if (pending) "ТЕСТ ЗАПУЩЕН ✓" else label
        }
    }

    private fun markTestStarted(key: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putLong(key, System.currentTimeMillis() + 60_000L)
            .apply()
        refreshTestButtons()
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

    override fun onPause() {
        countdownHandler.removeCallbacks(countdownTick)
        super.onPause()
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
        countdownHandler.removeCallbacks(countdownTick)
        countdownHandler.post(countdownTick)

        // A test button is only locked while its one-minute alarm is pending.
        // Returning from an alarm or reopening the app refreshes its state.
        refreshTestButtons()
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
