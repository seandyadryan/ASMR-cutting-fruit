package com.deploydulupulangnanti.asmrcuttingfruit

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.SoundPool

class StudioAudio(context: Context) {
    private val prefs = context.getSharedPreferences("studio", Context.MODE_PRIVATE)
    private val manager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private val pool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attributes).build()
    private val ready = mutableSetOf<Int>()
    private val sounds = intArrayOf(R.raw.juicy, R.raw.soft, R.raw.crisp).map { pool.load(context, it, 1) }
    private val room = pool.load(context, R.raw.ambience, 1)
    private var ambientStream = 0
    private var active = false
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener { change ->
            if (change < 0) { pool.autoPause() } else if (change > 0 && active) pool.autoResume()
        }.build()
    init { pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) { ready += id; if (id == room) updateAmbience() } } }
    fun resume() { active = true; manager.requestAudioFocus(focus); pool.autoResume(); updateAmbience() }
    fun pause() { active = false; pool.autoPause(); manager.abandonAudioFocusRequest(focus) }
    fun updateAmbience() {
        if (ambientStream != 0) { pool.stop(ambientStream); ambientStream = 0 }
        if (active && prefs.getBoolean("ambience", false) && prefs.getBoolean("sound", true) && room in ready)
            ambientStream = pool.play(room, .14f * volume(), .14f * volume(), 0, -1, 1f)
    }
    private fun volume() = prefs.getInt("volume", 75) / 100f
    fun cut(kind: Int) {
        if (!active || !prefs.getBoolean("sound", true)) return
        val id = sounds[kind]
        if (id in ready) {
            val pan = kotlin.random.Random.nextFloat() * .16f
            pool.play(id, volume() * (1 - pan), volume() * (.84f + pan), 1, 0, .94f + kotlin.random.Random.nextFloat() * .12f)
        }
    }
    fun close() { pause(); pool.release() }
}
