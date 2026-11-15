package com.yasin.ffvoice

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null
    private var panel: View? = null
    private lateinit var bp: WindowManager.LayoutParams

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val ch = "ffvoice"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(ch, "Overlay", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, ch).setContentTitle("YASIN FF VOICE চালু")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(1, n)

        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val d = resources.displayMetrics.density
        val size = (56 * d).toInt()
        bp = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = 20; y = 300
        }
        bubble = TextView(this).apply {
            text = "🎤"; textSize = 24f; gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor("#CCE53935")) }
            var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
            setOnTouchListener { _, e ->
                when (e.action) {
                    MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = bp.x; oy = bp.y; moved = false }
                    MotionEvent.ACTION_MOVE -> {
                        if (Math.abs(e.rawX - sx) > 10 || Math.abs(e.rawY - sy) > 10) moved = true
                        bp.x = ox + (e.rawX - sx).toInt(); bp.y = oy + (e.rawY - sy).toInt()
                        wm.updateViewLayout(this, bp)
                        panel?.let { hidePanel() }
                    }
                    MotionEvent.ACTION_UP -> if (!moved) togglePanel()
                }
                true
            }
        }
        wm.addView(bubble, bp)
    }

    private fun togglePanel() { if (panel == null) showPanel() else hidePanel() }

    private fun hidePanel() { panel?.let { wm.removeView(it) }; panel = null }

    private fun showPanel() {
        val d = resources.displayMetrics.density
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8, 8, 8, 8)
            background = GradientDrawable().apply { setColor(Color.parseColor("#EE222222")); cornerRadius = 16 * d } }
        val voices = Store.load(this)
        if (voices.isEmpty()) col.addView(TextView(this).apply { text = "কোনো ভয়েস নেই"; setTextColor(Color.WHITE) })
        voices.forEach { v ->
            col.addView(Button(this).apply {
                text = v.name; isAllCaps = false
                setOnClickListener { Player.toggle(this@OverlayService, v) {} }
            })
        }
        col.addView(Button(this).apply { text = "⏹ থামান"; setOnClickListener { Player.stop() } })
        val sv = ScrollView(this).apply { addView(col) }
        val h = (resources.displayMetrics.heightPixels * 0.55).toInt()
        val lp = WindowManager.LayoutParams((220 * d).toInt(), h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = bp.x + (64 * d).toInt(); y = bp.y
        }
        panel = sv; wm.addView(sv, lp)
    }

    override fun onDestroy() {
        hidePanel(); bubble?.let { wm.removeView(it) }; bubble = null
        Player.stop(); super.onDestroy()
    }
}
