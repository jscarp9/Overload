package com.overload.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/*
 * 0.14 effects: sound packs for the end of rest, set-complete effects and PR celebrations.
 */

/** Rest-over sound packs, synthesised like the web app's Web Audio notes. */
object SoundPack {
    private const val SR = 44100
    private class Note(val f: Double, val t: Double, val d: Double, val type: String, val v: Double)
    fun play(kind: String) = Thread {
        runCatching {
            val notes = when (kind) {
                "soft" -> listOf(Note(660.0, 0.0, .5, "sine", .2), Note(990.0, .18, .7, "sine", .15))
                "gong" -> listOf(Note(110.0, 0.0, 2.4, "sine", .5), Note(220.0, 0.0, 1.8, "tri", .18), Note(331.0, 0.0, 1.2, "sine", .08))
                "airhorn" -> listOf(0.0, .45).flatMap { t -> listOf(Note(415.0, t, .35, "saw", .22), Note(523.0, t, .35, "saw", .18), Note(622.0, t, .35, "saw", .14)) }
                "arcade" -> listOf(523.0, 659.0, 784.0, 1047.0).mapIndexed { i, f -> Note(f, i * .09, .12, "square", .14) }
                else -> return@runCatching
            }
            val len = ((notes.maxOf { it.t + it.d } + .1) * SR).toInt(); val buf = DoubleArray(len)
            notes.forEach { n -> val s0 = (n.t * SR).toInt(); val ns = (n.d * SR).toInt()
                for (i in 0 until ns) { val x = 2 * PI * n.f * i / SR; val ph = (n.f * i / SR) % 1.0
                    val w = when (n.type) { "square" -> if (sin(x) >= 0) 1.0 else -1.0; "saw" -> 2 * ph - 1; "tri" -> 4 * kotlin.math.abs(ph - .5) - 1; else -> sin(x) }
                    val env = if (i < SR * .02) i / (SR * .02) else exp(-5.0 * (i - SR * .02) / ns)
                    if (s0 + i < len) buf[s0 + i] += w * n.v * env } }
            val pcm = ShortArray(len) { (buf[it].coerceIn(-1.0, 1.0) * 32767 * .8).toInt().toShort() }
            val at = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
            at.write(pcm, 0, pcm.size); at.play(); Thread.sleep((len * 1000L / SR) + 200); at.release()
        }
    }.start()
}

/** Set complete effect (sparks, shockwave, confetti pop, lightning) near the bottom of the screen. */
@Composable
fun SetFxBurst(trigger: Int, kind: String) {
    if (trigger <= 0 || kind == "pulse") return
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.snapTo(0f); t.animateTo(1f, tween(800, easing = LinearEasing)) }
    if (t.value >= 1f) return
    val cols = listOf(Color(0xFFFFBE0B), Color(0xFFEF2B36), Color(0xFF2563FF), Color(0xFF16B65B), Color(0xFFD14DFF))
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2, size.height * .72f); val f = t.value
        when (kind) {
            "sparks", "pop" -> { val n = if (kind == "pop") 14 else 10
                repeat(n) { i -> val a = i * 2 * PI / n; val r = f * 90.dp.toPx(); drawCircle(cols[i % 5], (if (kind == "pop") 6f else 4f) * density * (1 - f * .6f), c + Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat()), alpha = 1 - f) } }
            "shock" -> drawCircle(Color.White, f * 120.dp.toPx(), c, alpha = (1 - f) * .8f, style = Stroke(4.dp.toPx() * (1 - f)))
            "lightning" -> viewBox(size.width / density, size.height / density) { val x = size.width / density / 2; val y = size.height / density * .72f
                fillP("M${x + 2} ${y - 22}L${x - 10} ${y}h9l-3 18 14-24h-9z", Color(0xFFFFF6A8), 1 - f) }
        }
    }
}

/** Fireworks and lightning storm PR celebrations. */
@Composable
fun FxBurst(trigger: Int, kind: String) {
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.snapTo(0f); t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    if (t.value >= 1f) return
    val pts = remember(trigger) { List(if (kind == "storm") 6 else 5) { Triple(Random.nextFloat(), Random.nextFloat() * .5f + .1f, Random.nextFloat() * .5f) } }
    Canvas(Modifier.fillMaxSize()) {
        pts.forEachIndexed { j, (x, y, d) ->
            val f = ((t.value - d) / .5f).coerceIn(0f, 1f); if (f <= 0f || f >= 1f) return@forEachIndexed
            val c = Offset(x * size.width, y * size.height)
            if (kind == "storm") { if (f < .3f) { drawRect(Color.White, alpha = .25f * (1 - f / .3f)); viewBox(size.width / density, size.height / density) { val cx = c.x / density; val cy = c.y / density
                    fillP("M${cx + 6} ${cy - 40}L${cx - 14} ${cy}h12l-6 30 22-40h-12z", Color(0xFFFFF6A8)) } } }
            else repeat(16) { i -> val a = i * 2 * PI / 16; val r = f * 110.dp.toPx()
                drawCircle(listOf(Color(0xFFFFBE0B), Color(0xFFEF2B36), Color(0xFF2563FF), Color(0xFFD14DFF))[(i + j) % 4], 3f * density, c + Offset((cos(a) * r).toFloat(), (sin(a) * r + f * f * 40 * density).toFloat()), alpha = 1 - f) }
        }
    }
}
