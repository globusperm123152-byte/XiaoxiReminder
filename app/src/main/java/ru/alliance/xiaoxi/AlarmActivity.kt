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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationId =
            intent.getIntExtra("notificationId", -1)

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
            text = "Сяоси"
            textSize = 42f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = """
                
                Пора сделать паузу
                
                Первый этап
                
                10 минут — закрыть глаза
                
                Затем:
                5 минут — спокойно походить
            """.trimIndent()

            textSize = 24f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
        }

        val startButton = Button(this).apply {
            text = "НАЧАТЬ"
            textSize = 20f

            setOnClickListener {
                stopAlarm()
                removeNotification()

                ReminderScheduler.scheduleExerciseTimer(
                    context = this@AlarmActivity,
                    minutes = 10,
                    stage = 1
                )

                finish()
            }
        }

        val postponeButton = Button(this).apply {
            text = "ОТЛОЖИТЬ / ПРОПУСТИТЬ"
            textSize = 17f

            setOnClickListener {
                showPostponeDialog()
            }
        }

        layout.addView(title)
        layout.addView(message)
        layout.addView(startButton)
        layout.addView(postponeButton)

        setContentView(layout)
    }

    private fun showPostponeDialog() {

        AlertDialog.Builder(this)
            .setTitle("Сяоси")
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

                        ReminderScheduler.scheduleSnooze(
                            context = this,
                            minutes = 10
                        )

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
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun startAlarm() {

        val alarmUri =
            RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

        ringtone = RingtoneManager.getRingtone(
            applicationContext,
            alarmUri
        )

        ringtone?.audioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
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
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }

        val pattern =
            longArrayOf(0, 700, 400, 700, 400)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    pattern,
                    0
                )
            )

        } else {

            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
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
