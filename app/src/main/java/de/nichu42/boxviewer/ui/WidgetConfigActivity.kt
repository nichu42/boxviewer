package de.nichu42.boxviewer.ui

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import de.nichu42.boxviewer.data.db.SenseBoxDatabase
import de.nichu42.boxviewer.data.repository.SenseBoxRepository
import de.nichu42.boxviewer.util.FontScaleHelper
import de.nichu42.boxviewer.util.LocaleHelper

import de.nichu42.boxviewer.ui.theme.MyApplicationTheme

class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleHelper.applySavedLocale(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Find the widget id from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If activity is launched without a widget id, finish immediately
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val db = SenseBoxDatabase.getDatabase(applicationContext)
        val repository = SenseBoxRepository(applicationContext, db)
        val appTextScale = FontScaleHelper.getSavedTextScale(this)

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                FontScaleHelper.ApplyTextScale(scale = appTextScale) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        WidgetConfigScreen(
                            repository = repository,
                            appWidgetId = appWidgetId,
                            onConfigSaved = {
                                val resultValue = Intent().apply {
                                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                }
                                setResult(RESULT_OK, resultValue)
                                finish()
                            },
                            onConfigCancelled = {
                                setResult(RESULT_CANCELED)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}
