package ru.alliance.xiaoxi

import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

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

        val button = Button(this).apply {
            text = "ВКЛЮЧИТЬ НАПОМИНАНИЯ"
            textSize = 17f

            setOnClickListener {
                text = "НАПОМИНАНИЯ ВКЛЮЧЕНЫ ✓"
                isEnabled = false
            }
        }

        layout.addView(title)
        layout.addView(info)
        layout.addView(button)

        setContentView(layout)
    }
}
