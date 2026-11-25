package com.overload.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.Notification

/**
 * Lock-screen rest alerts (port of web 0.11 push alerts).
 * The web app needs a push server because a backgrounded tab can't run timers. Here an exact
 * AlarmManager alarm fires RestAlarmReceiver at the end of rest, even with the phone locked.
 * The alarm is only set while the app is in the background and cancelled when it comes back.
 */
object RestAlerts {
    private const val CHANNEL = "rest"
    private const val NOTIF_ID = 4011
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_BODY = "body"

    fun canNotify(ctx: Context) = Build.VERSION.SDK_INT < 33 ||
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) != null) return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Rest alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "A ping when rest is over"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        })
    }

    fun show(ctx: Context, title: String, body: String) {
        if (!canNotify(ctx)) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(NOTIF_ID, n)
    }

    private fun alarmIntent(ctx: Context, title: String = "", body: String = "") = PendingIntent.getBroadcast(ctx, 0,
        Intent(ctx, RestAlarmReceiver::class.java).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_BODY, body),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun schedule(ctx: Context, at: Long, next: String) {
        val body = if (next.isNotEmpty()) "Next up: $next" else "Time for your next set 💪"
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = alarmIntent(ctx, "Rest is over ⏱️", body)
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi) // no exact-alarm access: may land a little late
    }

    fun cancel(ctx: Context) {
        ctx.getSystemService(AlarmManager::class.java).cancel(alarmIntent(ctx))
        ctx.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
    }

    internal fun fromIntent(ctx: Context, i: Intent) =
        show(ctx, i.getStringExtra(EXTRA_TITLE) ?: "Rest is over ⏱️", i.getStringExtra(EXTRA_BODY) ?: "Time for your next set 💪")
}

class RestAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) = RestAlerts.fromIntent(ctx, intent)
}
