package ru.gdesanek
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "ГдеСанёк жив! Шеф, поехали."
            textSize = 24f
        })
    }
}
