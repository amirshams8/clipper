package ca.zgrs.clipper

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class Main : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        startService(Intent(this, ClipboardService::class.java))
    }
}
