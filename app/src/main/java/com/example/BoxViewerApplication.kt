package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.example.widget.SenseBoxWidgetProvider

class BoxViewerApplication : Application() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                SenseBoxWidgetProvider.updateAllWidgets(context, force = false)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Register dynamic screen-on broadcast receiver to update widgets when the device wakes up/screen is active.
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
