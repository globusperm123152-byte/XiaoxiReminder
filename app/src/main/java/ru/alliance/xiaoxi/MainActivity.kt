package ru.alliance.xiaoxi

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = TextView(this).apply {
            text = "Сяоси работает ✓"
            textSize = 28f
            setPadding(60, 100, 60, 60)
        }

        setContentView(text)
    }
}
