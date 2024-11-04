package com.loohp.hkbuseta

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.glance.appwidget.updateAll
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.loohp.hkbuseta.appcontext.HistoryStack
import com.loohp.hkbuseta.appcontext.componentActivityPaused
import com.loohp.hkbuseta.appcontext.context
import com.loohp.hkbuseta.appcontext.setApplicationContext
import com.loohp.hkbuseta.appcontext.setComponentActivity
import com.loohp.hkbuseta.common.appcontext.AppIntent
import com.loohp.hkbuseta.common.appcontext.AppScreen
import com.loohp.hkbuseta.common.external.extractShareLink
import com.loohp.hkbuseta.common.external.shareLaunch
import com.loohp.hkbuseta.common.shared.Shared
import com.loohp.hkbuseta.common.shared.Tiles
import com.loohp.hkbuseta.common.utils.remove
import com.loohp.hkbuseta.glance.FavouriteRoutesWidget
import com.loohp.hkbuseta.shared.AndroidShared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var pipModeState: PipModeState = PipModeState.Left

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        setApplicationContext(applicationContext)
        setComponentActivity(this)
        AndroidShared.setDefaultExceptionHandler()
        AndroidShared.scheduleBackgroundUpdateService(this)
        Shared.provideBackgroundUpdateScheduler { c, t -> AndroidShared.scheduleBackgroundUpdateService(c.context, t) }
        Tiles.providePlatformUpdate {
            CoroutineScope(Dispatchers.Main).launch {
                FavouriteRoutesWidget.updateAll(this@MainActivity)
            }
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "alight_reminder_channel",
            resources.getString(R.string.alight_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            enableVibration(true)
            enableLights(true)
        }
        notificationManager.createNotificationChannel(channel)
        setContent {
            App()
        }
        CoroutineScope(Dispatchers.IO).launch {
            intent.extractUrl()?.extractShareLink()?.apply {
                val instance = HistoryStack.historyStack.value.last()
                instance.startActivity(AppIntent(instance, AppScreen.MAIN))
                instance.finishAffinity()
                delay(500)
                shareLaunch(instance, true)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        CoroutineScope(Dispatchers.IO).launch {
            intent.extractUrl()?.extractShareLink()?.apply {
                val instance = HistoryStack.historyStack.value.last()
                instance.startActivity(AppIntent(instance, AppScreen.MAIN))
                instance.finishAffinity()
                delay(500)
                shareLaunch(instance, true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        componentActivityPaused = true
    }

    override fun onResume() {
        super.onResume()
        componentActivityPaused = false
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        if (isInPictureInPictureMode) {
            pipModeState = PipModeState.Entered
        } else {
            val id = Random.nextInt()
            pipModeState = PipModeState.JustLeft(id)
            CoroutineScope(Dispatchers.IO).launch {
                delay(1500)
                if (pipModeState.matchesJustLeft(id)) {
                    pipModeState = PipModeState.Left
                }
            }
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        if (pipModeState is PipModeState.Left) {
            recreate()
        }
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

}

internal fun Intent.extractUrl(): String? {
    return when (action) {
        Intent.ACTION_SEND -> if (type == "text/plain") getStringExtra(Intent.EXTRA_TEXT)?.remove("\n") else null
        Intent.ACTION_VIEW -> data.toString()
        else -> null
    }
}

private sealed interface PipModeState {
    data object Entered: PipModeState
    data class JustLeft(val id: Int): PipModeState
    data object Left: PipModeState
}

private fun PipModeState.matchesJustLeft(id: Int): Boolean {
    return this is PipModeState.JustLeft && this.id == id
}