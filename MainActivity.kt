package com.yasin.ffvoice

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.animation.OvershootInterpolator
import android.widget.*

class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private val PICK = 1
    private val BG = Color.parseColor("#0D0D14")
    private val CARD = Color.parseColor("#1B1B2B")
    private val RED = Color.parseColor("#E53935")
    private val PURPLE = Color.parseColor("#7C4DFF")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun shape(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(r).toFloat()
    }

    private fun gradient(a: Int, b: Int, r: Int) =
        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(a, b)).apply {
            cornerRadius = dp(r).toFloat()
        }

    private fun pill(label: String, a: Int, b: Int, onClick: () -> Unit) = TextView(this).apply {
        text = label; setTextColor(Color.WHITE); textSize = 13f
        typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
        setPadding(dp(8), dp(12), dp(8), dp(12))
        background = gradient(a, b, 14)
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins(dp(4), 0, dp(4), 0) }
        setOnClickListener { onClick() }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(16), dp(40), dp(16), dp(16))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = gradient(RED, PURPLE, 20)
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        header.addView(TextView(this).apply {
            text = "🎤 YASIN FF VOICE"; textSize = 24f
            setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
        })
        header.addView(TextView(this).apply {
            text = "Your voice. Your soundboard."; textSize = 13f
            setTextColor(Color.parseColor("#DDFFFFFF"))
        })
        root.addView(header)

        val row = LinearLayout(this).apply { setPadding(0, dp(14), 0, dp(6)) }
        row.addView(pill("+ ADD VOICE", PURPLE, Color.parseColor("#536DFE")) { pick() })
        row.addView(pill("OVERLAY ON", Color.parseColor("#2E7D32"), Color.parseColor("#43A047")) { startOverlay() })
        row.addView(pill("OVERLAY OFF", Color.parseColor("#B71C1C"), RED) {
            stopService(Intent(this@MainActivity, OverlayService::class.java))
        })
        root.addView(row)

        val row2 = LinearLayout(this).apply { setPadding(0, 0, 0, dp(6)) }
        row2.addView(pill("🎯 FREE FIRE SENSITIVITY", Color.parseColor("#F57C00"), Color.parseColor("#FFB300")) { sens() })
        root.addView(row2)

        root.addView(TextView(this).apply {
            text = "ট্যাপ = বাজবে, আবার ট্যাপ = থামবে • লং-প্রেস = নাম বদল/ডিলিট\nগেমে শোনাতে ফোনের মিডিয়া ভলিউম বেশি রাখুন"
            textSize = 11f; setTextColor(Color.parseColor("#8888AA"))
            setPadding(dp(4), dp(4), dp(4), dp(10))
        })

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) })

        val host = FrameLayout(this)
        host.addView(root)
        setContentView(host)
        refresh()
        if (b == null) splash(host)
    }

    private fun splash(host: FrameLayout) {
        val s = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(BG); isClickable = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = shape(CARD, 36); clipToOutline = true
            layoutParams = LinearLayout.LayoutParams(dp(160), dp(160))
            alpha = 0f; scaleX = 0.5f; scaleY = 0.5f
        }
        val title = TextView(this).apply {
            text = "YASIN FF VOICE"; textSize = 26f
            setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(24), 0, 0)
            alpha = 0f; translationY = dp(20).toFloat()
        }
        val tag = TextView(this).apply {
            text = "Your voice. Your soundboard."; textSize = 13f
            setTextColor(Color.parseColor("#8888AA"))
            alpha = 0f; translationY = dp(20).toFloat()
        }
        s.addView(logo); s.addView(title); s.addView(tag)
        host.addView(s)

        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700)
            .setInterpolator(OvershootInterpolator()).start()
        title.animate().alpha(1f).translationY(0f).setStartDelay(450).setDuration(500).start()
        tag.animate().alpha(1f).translationY(0f).setStartDelay(700).setDuration(500).start()
        s.postDelayed({
            s.animate().alpha(0f).setDuration(400).withEndAction { host.removeView(s) }.start()
        }, 2000)
    }

    private fun sens() {
        val mi = ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
        val ram = mi.totalMem / 1073741824.0
        val ramText = String.format("%.1f", ram)
        val names = arrayOf("Low-end", "Mid-range", "High-end")
        val auto = if (ram <= 3.5) 0 else if (ram <= 6.5) 1 else 2
        AlertDialog.Builder(this)
            .setTitle("🎯 ${Build.MANUFACTURER} ${Build.MODEL}")
            .setMessage("RAM প্রায় $ramText GB\nসাজেস্টেড টিয়ার: ${names[auto]}")
            .setPositiveButton("দেখুন") { _, _ -> showSens(auto, names) }
            .setNeutralButton("অন্য টিয়ার") { _, _ ->
                AlertDialog.Builder(this).setItems(names) { _, w -> showSens(w, names) }.show()
            }.show()
    }

    private fun showSens(t: Int, names: Array<String>) {
        val v = arrayOf(
            arrayOf(100, 95, 90, 85, 80, 90),
            arrayOf(95, 92, 88, 82, 75, 85),
            arrayOf(90, 88, 85, 78, 70, 80)
        )[t]
        val labels = arrayOf("General", "Red Dot", "2x Scope", "4x Scope", "AWM Scope", "Free Look")
        val msg = labels.indices.joinToString("\n") { "${labels[it]}: ${v[it]}" } +
            "\n\nএগুলো শুধু শুরুর পয়েন্ট, হেডশটের গ্যারান্টি নয়। ট্রেনিং গ্রাউন্ডে একটু একটু বাড়িয়ে-কমিয়ে নিজের হাতের সাথে মিলিয়ে নিন।"
        AlertDialog.Builder(this).setTitle("${names[t]} সেনসিটিভিটি")
            .setMessage(msg).setPositiveButton("OK", null).show()
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
        if (voices.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "\nএখনো কোনো ভয়েস নেই।\n\"+ ADD VOICE\" চেপে অডিও যোগ করুন।"
                setTextColor(Color.parseColor("#8888AA")); gravity = Gravity.CENTER
                setPadding(0, dp(40), 0, 0)
            })
        }
        voices.forEach { v ->
            val playing = Player.currentId == v.id
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    setColor(if (playing) Color.parseColor("#2A1B2B") else CARD)
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(2), if (playing) RED else Color.TRANSPARENT)
                }
                setPadding(dp(14), dp(14), dp(14), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, dp(10)) }
                setOnClickListener {
                    Player.toggle(this@MainActivity, v) { runOnUiThread { refresh() } }
                    refresh()
                }
                setOnLongClickListener { options(v); true }
            }
            card.addView(TextView(this).apply {
                text = if (playing) "⏸" else "▶"
                setTextColor(Color.WHITE); textSize = 16f; gravity = Gravity.CENTER
                background = shape(if (playing) RED else PURPLE, 24)
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { setMargins(0, 0, dp(14), 0) }
            })
            card.addView(TextView(this).apply {
                text = v.name; setTextColor(Color.WHITE); textSize = 15f
                typeface = Typeface.DEFAULT_BOLD; maxLines = 2
            })
            list.addView(card)
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
