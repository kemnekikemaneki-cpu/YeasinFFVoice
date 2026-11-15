package com.yasin.ffvoice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Voice(val id: String, var name: String, val file: String)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("voices", Context.MODE_PRIVATE)
    private fun dir(c: Context) = File(c.filesDir, "voices").apply { mkdirs() }
    fun fileOf(c: Context, v: Voice) = File(dir(c), v.file)

    fun load(c: Context): MutableList<Voice> {
        val arr = JSONArray(prefs(c).getString("list", "[]"))
        return MutableList(arr.length()) {
            val o = arr.getJSONObject(it)
            Voice(o.getString("id"), o.getString("name"), o.getString("file"))
        }
    }

    fun save(c: Context, list: List<Voice>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("file", it.file)) }
        prefs(c).edit().putString("list", arr.toString()).apply()
    }

    fun add(c: Context, uri: Uri): Boolean = try {
        var name = "Voice"
        c.contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) name = it.getString(i).substringBeforeLast('.')
            }
        }
        val id = UUID.randomUUID().toString()
        val out = File(dir(c), id)
        c.contentResolver.openInputStream(uri)!!.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
        val list = load(c); list.add(Voice(id, name, id)); save(c, list)
        true
    } catch (e: Exception) { false }

    fun delete(c: Context, v: Voice) {
        fileOf(c, v).delete()
        save(c, load(c).filter { it.id != v.id })
    }

    fun rename(c: Context, v: Voice, name: String) {
        val l = load(c); l.find { it.id == v.id }?.name = name; save(c, l)
    }
}

object Player {
    private var mp: MediaPlayer? = null
    var currentId: String? = null

    fun toggle(c: Context, v: Voice, onEnd: () -> Unit) {
        if (currentId == v.id) { stop(); onEnd(); return }
        stop()
        try {
            mp = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(Store.fileOf(c, v).absolutePath)
                setOnCompletionListener { Player.stop(); onEnd() }
                prepare(); start()
            }
            currentId = v.id
        } catch (e: Exception) { stop(); onEnd() }
    }

    fun stop() {
        mp?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        mp = null; currentId = null
    }
}
