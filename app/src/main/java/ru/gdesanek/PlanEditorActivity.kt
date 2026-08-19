package ru.gdesanek

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.gdesanek.ui.PlanView

class PlanEditorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val planView = PlanView(this)
        setContentView(planView)
    }
}
