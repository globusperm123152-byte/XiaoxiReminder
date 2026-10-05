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
    private lateinit var stopButton: Button
    private val countdownHandler = Handler(Looper.getMainLooper())
    private val countdownTick = object : Runnable {
        override fun run() {
            updateCountdown()
            countdownHandler.postDelayed(this, 1000L)
        }
    }
    private val timeButtons = mutableMapOf<String, TextView>()
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private val ink = Color.rgb(242, 233, 217)
    private val muted = Color.rgb(181, 169, 151)
    private val canvasColor = Color.rgb(37, 39, 40)
    private val accent = Color.rgb(209, 165, 108)

    private fun shape(color: Int, radius: Int = 18, border: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            cornerRadius = dp(radius).toFloat()
            setColor(color)
            if (border != null) setStroke(dp(1), border)
        }

    private fun createInterface() {
        window.statusBarColor = canvasColor
        window.navigationBarColor = canvasColor
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            0

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(32), dp(22), dp(42))
            setBackgroundColor(canvasColor)
        }

        fun label(
            parent: LinearLayout, value: String, size: Float,
            color: Int = ink, bold: Boolean = false, bottom: Int = 0
        ): TextView {
            val view = TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                if (bold) setTypeface(null, Typeface.BOLD)
                includeFontPadding = false
                letterSpacing = if (size <= 13f) 0.07f else 0f
            }
            parent.addView(view, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(bottom) })
            return view
        }

        fun card(background: Int = Color.rgb(51, 53, 53)): LinearLayout {
            val block = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                this.background = shape(background, 20, Color.rgb(85, 80, 73))
            }
            page.addView(block, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) })
            return block
        }

        fun timeChip(parent: LinearLayout, key: String, h: Int, min: Int) {
            val chip = TextView(this).apply {
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(ink)
                background = shape(Color.rgb(70, 69, 65), 13)
                setPadding(dp(18), dp(12), dp(18), dp(12))
                isClickable = true
                isFocusable = true
                contentDescription = "Изменить время"
                setOnClickListener {
                    val current = readTime(key, h, min)
                    TimePickerDialog(
                        this@MainActivity,
                        { _, hour, minute ->
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit().putInt(key, hour * 60 + minute).apply()
                            updateTimeButtons()
                            if (getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                    .getBoolean(KEY_REMINDERS_ENABLED, false)) {
                                ReminderScheduler.scheduleSavedTime(this@MainActivity, key)
                            }
                        }, current.first, current.second, true
                    ).show()
                }
            }
            timeButtons[key] = chip
            parent.addView(chip, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = dp(8)
                bottomMargin = dp(9)
            })
        }

        label(page, "СЯОСИ", 12f, accent, true, 9)
        label(page, "Время замедлиться.", 29f, ink, true, 7)
        label(page, "Небольшие паузы. Больше ясности.", 15f, muted, false, 26)

        val currentCard = card(Color.rgb(63, 61, 57))
        label(currentCard, "ТЕКУЩАЯ ПРАКТИКА", 11f, accent, true, 12)
        countdownLabel = label(currentCard, "Всё спокойно", 29f, ink, true, 7)
        label(currentCard, "Дыши ровно. Всё идёт своим чередом.", 13f, muted)
        stopButton = Button(this).apply {
            text = "Остановить упражнение"
            isAllCaps = false
            textSize = 15f
            setTextColor(ink)
            backgroundTintList = ColorStateList.valueOf(Color.rgb(91, 83, 72))
            setOnClickListener {
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Завершить упражнение?")
                    .setMessage("Обратный отсчёт и сигнал окончания будут отменены.")
                    .setNegativeButton("Продолжить", null)
                    .setPositiveButton("Остановить") { _, _ ->
                        ReminderScheduler.stopActivePractice(this@MainActivity)
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .remove("countdown_xiaoxi_eyes")
                            .remove("countdown_xiaoxi_walk")
                            .remove("countdown_xiaoshi")
                            .remove("countdown_qigong")
                            .apply()
                        updateCountdown()
                    }
                    .show()
            }
        }
        currentCard.addView(stopButton)

        val xiaoxi = card()
        label(xiaoxi, "01   /   ВОССТАНОВЛЕНИЕ", 11f, accent, true, 10)
        label(xiaoxi, "Сяоси", 24f, ink, true, 7)
        label(xiaoxi, "Закрой глаза на 10 минут, затем пройдись 5 минут.", 14f, muted, false, 17)
        label(xiaoxi, "ПОНЕДЕЛЬНИК — ПЯТНИЦА", 11f, muted, true, 12)
        val timeGrid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        xiaoxi.addView(timeGrid)
        val defaults = listOf(11 to 0, 12 to 30, 14 to 0, 15 to 30, 17 to 0)
        for (rowIndex in 0..2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            timeGrid.addView(row)
            for (col in 0..1) {
                val index = rowIndex * 2 + col
                if (index < defaults.size) {
                    val time = defaults[index]
                    timeChip(row, "time_xiaoxi_$index", time.first, time.second)
                }
            }
        }

        val xiaoshi = card()
        label(xiaoshi, "02   /   ОТДЫХ", 11f, Color.rgb(191, 158, 129), true, 10)
        label(xiaoshi, "Сяоши", 24f, ink, true, 7)
        label(xiaoshi, "25 минут дневного сна", 14f, muted, false, 16)
        timeChip(xiaoshi, "time_xiaoshi", 13, 30)

        val qigong = card()
        label(qigong, "03   /   ДВИЖЕНИЕ", 11f, Color.rgb(191, 166, 125), true, 10)
        label(qigong, "Цигун", 24f, ink, true, 7)
        label(qigong, "5 минут мягкого движения и дыхания", 14f, muted, false, 16)
        timeChip(qigong, "time_qigong", 16, 30)

        label(page, "Нажми на время, чтобы изменить расписание.", 13f, muted, false, 18)

        val controls = card()
        label(controls, "УПРАВЛЕНИЕ", 11f, muted, true, 12)
        fun actionButton(onPress: () -> Unit): Button =
            Button(this).apply {
                isAllCaps = false
                textSize = 14f
                setTextColor(ink)
                backgroundTintList = ColorStateList.valueOf(Color.rgb(91, 83, 72))
                setOnClickListener { onPress() }
            }
        enableButton = actionButton { enableReminders() }
        testButton = actionButton { startTest() }
        xiaoshiTestButton = actionButton { startXiaoshiTest() }
        qigongTestButton = actionButton { startQigongTest() }
        listOf(enableButton, testButton, xiaoshiTestButton, qigongTestButton)
            .forEach {
                controls.addView(it, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(5) })
            }

        setContentView(ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
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
        if (::stopButton.isInitialized) {
            stopButton.visibility = if (current == null)
                android.view.View.GONE else android.view.View.VISIBLE
        }
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
