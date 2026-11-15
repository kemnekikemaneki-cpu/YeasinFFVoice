package com.yasin.ffvoice

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private val PICK = 1

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
        val p = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(p, p * 2, p, p) }
        root.addView(TextView(this).apply { text = "YASIN FF VOICE"; textSize = 24f; setTextColor(Color.BLACK) })
        val row = LinearLayout(this)
        row.addView(Button(this).apply { text = "+ Add voice"; setOnClickListener { pick() } })
        row.addView(Button(this).apply { text = "Overlay ON"; setOnClickListener { startOverlay() } })
        row.addView(Button(this).apply { text = "Overlay OFF"; setOnClickListener { stopService(Intent(this@MainActivity, OverlayService::class.java)) } })
        root.addView(row)
        root.addView(TextView(this).apply {
            text = "ট্যাপ = বাজবে, আবার ট্যাপ = থামবে। লং-প্রেস = নাম বদল/ডিলিট।\nগেমে শোনাতে ফোনের মিডিয়া ভলিউম বেশি রাখুন।"
            textSize = 12f
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) })
        setContentView(root)
        refresh()
    }

    private fun pick() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(i, PICK)
    }

    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        if (rc == PICK && res == RESULT_OK && d != null) {
            val uris = mutableListOf<Uri>()
            d.data?.let { uris.add(it) }
            d.clipData?.let { for (k in 0 until it.itemCount) uris.add(it.getItemAt(k).uri) }
            var ok = 0
            uris.distinct().forEach { if (Store.add(this, it)) ok++ }
            Toast.makeText(this, "$ok টি ভয়েস যোগ হয়েছে", Toast.LENGTH_SHORT).show()
            refresh()
        }
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Overlay পারমিশন দিন, তারপর আবার চালু করুন", Toast.LENGTH_LONG).show()
            return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        Toast.makeText(this, "Overlay চালু। এবার গেমে যান", Toast.LENGTH_SHORT).show()
    }

    private fun refresh() {
        list.removeAllViews()
        val voices = Store.load(this)
        if (voices.isEmpty()) list.addView(TextView(this).apply { text = "\nএখনো কোনো ভয়েস নেই। \"+ Add voice\" চাপুন।" })
        voices.forEach { v ->
            val btn = Button(this).apply {
                text = "▶  ${v.name}"; isAllCaps = false
                setOnClickListener { Player.toggle(this@MainActivity, v) { runOnUiThread { } } }
                setOnLongClickListener { options(v); true }
            }
            list.addView(btn)
        }
    }

    private fun options(v: Voice) {
        AlertDialog.Builder(this).setTitle(v.name).setItems(arrayOf("নাম বদলান", "ডিলিট")) { _, w ->
            if (w == 1) { Player.stop(); Store.delete(this, v); refresh() }
            else {
                val et = EditText(this).apply { setText(v.name) }
                AlertDialog.Builder(this).setView(et).setPositiveButton("OK") { _, _ ->
                    Store.rename(this, v, et.text.toString().ifBlank { v.name }); refresh()
                }.show()
            }
        }.show()
    }

    override fun onResume() { super.onResume(); refresh() }
}
