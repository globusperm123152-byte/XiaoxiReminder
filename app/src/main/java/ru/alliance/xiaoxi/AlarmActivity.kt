package ru.alliance.xiaoxi

import android.app.NotificationManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AlarmActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var notificationId: Int = -1

    private var alarmType: String = "regular"
    private var stage: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationId =
            intent.getIntExtra("notificationId", -1)

        alarmType =
            intent.getStringExtra("alarmType") ?: "regular"

        stage =
            intent.getIntExtra("stage", 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        createInterface()
        startAlarm()
    }

    private fun createInterface() {

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = when {
                isXiaoshi() -> "Сяоши"
                isQigong() -> "Цигун"
                else -> "Сяоси"
            }

            textSize = 42f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = getMessage()
            textSize = 24f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        val mainButton = Button(this).apply {
            text = getMainButtonText()
            textSize = 20f

            setOnClickListener {
                handleMainButton()
            }
        }

        layout.addView(title)
        layout.addView(message)
        layout.addView(mainButton)

        if (
            alarmType == "regular" ||
            alarmType == "snooze" ||
            alarmType == "xiaoshi" ||
            alarmType == "xiaoshi_snooze" ||
            alarmType == "qigong" ||
            alarmType == "qigong_snooze"
        ) {

            val postponeButton = Button(this).apply {
                text = "ОТЛОЖИТЬ / ПРОПУСТИТЬ"
                textSize = 17f

                setOnClickListener {
                    showPostponeDialog()
                }
            }

            layout.addView(postponeButton)
        }

        setContentView(layout)
    }

    private fun isXiaoshi(): Boolean {

        return (
            alarmType == "xiaoshi" ||
            alarmType == "xiaoshi_timer" ||
            alarmType == "xiaoshi_snooze"
        )
    }

    private fun isQigong(): Boolean {

        return (
            alarmType == "qigong" ||
            alarmType == "qigong_timer" ||
            alarmType == "qigong_snooze"
        )
    }

    private fun getMessage(): String {

        return when {

            alarmType == "qigong_timer" -> """
                
                Цигун завершён ✓
                
                5 минут закончились
                
                Можно возвращаться к работе
            """.trimIndent()

            alarmType == "qigong_snooze" -> """
                
                Отложенный Цигун
                
                Пора сделать практику
                
                5 минут
                
                Спокойное дыхание и движение
            """.trimIndent()

            alarmType == "qigong" -> """
                
                Время Цигун
                
                5 минут
                
                Спокойное дыхание
                и плавные движения
                
                Убери телефон и отвлекись от работы
            """.trimIndent()

            alarmType == "xiaoshi_timer" -> """
                
                Сяоши завершена ✓
                
                25 минут отдыха закончились
                
                Можно спокойно возвращаться
                к работе
            """.trimIndent()

            alarmType == "xiaoshi_snooze" -> """
                
                Отложенная Сяоши
                
                Пора сделать дневной отдых
                
                25 минут
            """.trimIndent()

            alarmType == "xiaoshi" -> """
                
                Время Сяоши
                
                Дневной сон
                
                25 минут
                
                Убери телефон и закрой глаза
            """.trimIndent()

            alarmType == "exercise" && stage == 1 -> """
                
                Первый этап завершён ✓
                
                Теперь встань и спокойно походи
                
                5 минут
                
                Без телефона и без работы
            """.trimIndent()

            alarmType == "exercise" && stage == 2 -> """
                
                Сяоси завершена ✓
                
                10 минут — глаза закрыты
                5 минут — спокойная ходьба
                
                Можно возвращаться к работе
            """.trimIndent()

            alarmType == "snooze" -> """
                
                Напоминание было отложено
                
                Пора сделать паузу
                
                10 минут — закрыть глаза
                затем
                5 минут — спокойно походить
            """.trimIndent()

            else -> """
                
                Пора сделать паузу
                
                10 минут — закрыть глаза
                
                Затем:
                5 минут — спокойно походить
            """.trimIndent()
        }
    }

    private fun getMainButtonText(): String {

        return when {

            alarmType == "qigong_timer" ->
                "ГОТОВО"

            alarmType == "xiaoshi_timer" ->
                "ГОТОВО"

            alarmType == "exercise" && stage == 1 ->
                "НАЧАТЬ 5 МИНУТ ХОДЬБЫ"

            alarmType == "exercise" && stage == 2 ->
                "ГОТОВО"

            else ->
                "НАЧАТЬ"
        }
    }

    private fun handleMainButton() {

        stopAlarm()
        removeNotification()

        when {

            alarmType == "qigong_timer" -> {
                finish()
            }

            alarmType == "qigong" ||
            alarmType == "qigong_snooze" -> {

                ReminderScheduler.scheduleQigongTimer(
                    context = this,
                    minutes = 5
                )

                finish()
            }

            alarmType == "xiaoshi_timer" -> {
                finish()
            }

            alarmType == "xiaoshi" ||
            alarmType == "xiaoshi_snooze" -> {

                ReminderScheduler.scheduleXiaoshiTimer(
                    context = this,
                    minutes = 25
                )

                finish()
            }

            alarmType == "exercise" && stage == 1 -> {

                ReminderScheduler.scheduleExerciseTimer(
                    context = this,
                    minutes = 5,
                    stage = 2
                )

                finish()
            }

            alarmType == "exercise" && stage == 2 -> {
                finish()
            }

            else -> {

                ReminderScheduler.scheduleExerciseTimer(
                    context = this,
                    minutes = 10,
                    stage = 1
                )

                finish()
            }
        }
    }

    private fun showPostponeDialog() {

        val dialogTitle = when {
            isXiaoshi() -> "Сяоши"
            isQigong() -> "Цигун"
            else -> "Сяоси"
        }

        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setItems(
                arrayOf(
                    "Отложить на 10 минут",
                    "Пропустить сейчас"
                )
            ) { dialog, which ->

                when (which) {

                    0 -> {

                        stopAlarm()
                        removeNotification()

                        when {
                            isXiaoshi() -> {
                                ReminderScheduler
                                    .scheduleXiaoshiSnooze(
                                        context = this,
                                        minutes = 10
                                    )
                            }

                            isQigong() -> {
                                ReminderScheduler
                                    .scheduleQigongSnooze(
                                        context = this,
                                        minutes = 10
                                    )
                            }

                            else -> {
                                ReminderScheduler
                                    .scheduleSnooze(
                                        context = this,
                                        minutes = 10
                                    )
                            }
                        }

                        finish()
                    }

                    1 -> {
                        stopAlarm()
                        removeNotification()
                        finish()
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton(
                "Отмена",
                null
            )
            .show()
    }

    private fun startAlarm() {

        val alarmUri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

        ringtone =
            RingtoneManager.getRingtone(
                applicationContext,
                alarmUri
            )

        ringtone?.audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_ALARM
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ringtone?.isLooping = true
        }

        ringtone?.play()

        vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                getSystemService(
                    VibratorManager::class.java
                ).defaultVibrator

            } else {

                @Suppress("DEPRECATION")
                getSystemService(
                    VIBRATOR_SERVICE
                ) as Vibrator
            }

        val pattern =
            longArrayOf(
                0,
                700,
                400,
                700,
                400
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    pattern,
                    0
                )
            )

        } else {

            @Suppress("DEPRECATION")
            vibrator?.vibrate(
                pattern,
                0
            )
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    private fun removeNotification() {

        if (notificationId >= 0) {

            getSystemService(
                NotificationManager::class.java
            ).cancel(notificationId)
        }
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }
}
