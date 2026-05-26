package com.example

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.widget.SenseBoxWidgetProvider
import com.example.util.CrashHandler

class BoxViewerApplication : Application() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON || intent.action == Intent.ACTION_USER_PRESENT) {
                SenseBoxWidgetProvider.updateAllWidgets(context, force = true)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize human-readable local uncaught exception handler/diagnostics logic.
        CrashHandler.init(this)
        
        // Register dynamic screen and user present broadcast receiver to update widgets when the device wakes up.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        
        // Register without flags since this is exclusively for system-protected broadcasts.
        // On Android 14+, specifying RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED for protected system-only actions
        // like ACTION_SCREEN_ON is not required and can cause security/delivery limitations.
        try {
            registerReceiver(screenReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

