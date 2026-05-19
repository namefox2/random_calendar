package com.randomcalendar.ui.timer

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.*
import androidx.core.app.NotificationCompat
import com.randomcalendar.R
import com.randomcalendar.RandomCalendarApp
import com.randomcalendar.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TimerService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            TimerManager.tick()
            updateNotification()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        toneGenerator = try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) { null }

        TimerManager.onAlarm = {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
        }

        TimerManager.onPersist = { todoId, seconds ->
            val db = (application as RandomCalendarApp).database
            serviceScope.launch {
                db.todoItemDao().updateElapsedSeconds(todoId, seconds)
            }
        }

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                handler.removeCallbacks(tickRunnable)
                startForeground(NOTIF_ID, buildNotification())
                handler.post(tickRunnable)
            }
            ACTION_PAUSE -> TimerManager.pause()
            ACTION_STOP -> {
                TimerManager.clear()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        toneGenerator?.release()
        toneGenerator = null
        TimerManager.onAlarm = null
        TimerManager.onPersist = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun buildNotification(): Notification {
        val state = TimerManager.state.value
        val title = "타이머 실행 중"
        val text = if (state != null) {
            val elapsed = TimerManager.formatSeconds(state.elapsedSeconds)
            if (state.timerType == "SET") {
                "${if (state.isWorkPhase) "운동" else "휴식"} · ${state.currentSet}/${state.totalSets}세트 · $elapsed"
            } else {
                elapsed
            }
        } else ""

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_timer_notif)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "타이머", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "타이머 실행 알림"
            setShowBadge(false)
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIF_ID = 1001
        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_STOP = "action_stop"

        fun start(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
