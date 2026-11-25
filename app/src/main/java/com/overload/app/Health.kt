package com.overload.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.math.ceil

/**
 * Heart rate from Samsung Health (or any watch app) via Health Connect.
 * Samsung Health copies its data into Health Connect every so often, not live, so a workout's
 * heart rate can show up a few minutes after it ends. The VM re-checks for a while after.
 */
object Health {
    private const val PROVIDER = "com.google.android.apps.healthdata"
    val PERMS = setOf(HealthPermission.getReadPermission(HeartRateRecord::class))

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    /** AVAILABLE, NEEDS_INSTALL (Health Connect missing/outdated) or UNSUPPORTED (Android 8). */
    fun status(ctx: Context): String = when (HealthConnectClient.getSdkStatus(ctx, PROVIDER)) {
        HealthConnectClient.SDK_AVAILABLE -> "AVAILABLE"
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> "NEEDS_INSTALL"
        else -> if (android.os.Build.VERSION.SDK_INT >= 28) "NEEDS_INSTALL" else "UNSUPPORTED"
    }

    fun openStore(ctx: Context) = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding"))
            .setPackage("com.android.vending").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {}

    suspend fun hasPerms(ctx: Context): Boolean = try {
        HealthConnectClient.getOrCreate(ctx).permissionController.getGrantedPermissions().containsAll(PERMS)
    } catch (_: Exception) { false }

    /** Reads heart rate between start and end. null = couldn't read (no access / not installed). */
    suspend fun read(ctx: Context, start: Long, end: Long): HeartRate? = try {
        val client = HealthConnectClient.getOrCreate(ctx)
        val samples = mutableListOf<Pair<Long, Long>>() // time ms, bpm
        var token: String? = null
        do {
            val resp = client.readRecords(ReadRecordsRequest(HeartRateRecord::class,
                TimeRangeFilter.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)), pageToken = token))
            resp.records.forEach { r -> r.samples.forEach { s -> val t = s.time.toEpochMilli(); if (t in start..end) samples += t to s.beatsPerMinute } }
            token = resp.pageToken
        } while (token != null)
        summarize(samples, start, end)
    } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null }

    /** Averages samples into at most ~90 buckets so a long workout's graph stays small to store. */
    private fun summarize(samples: List<Pair<Long, Long>>, start: Long, end: Long): HeartRate {
        val now = System.currentTimeMillis()
        if (samples.isEmpty()) return HeartRate(fetchedAt = now, forEnd = end)
        val step = maxOf(15, ceil((end - start) / 1000.0 / 90).toInt())
        val pts = samples.groupBy { ((it.first - start) / 1000 / step).toInt() }.toSortedMap()
            .map { (b, l) -> HrPt(b * step + step / 2, l.map { it.second }.average().toInt()) }
        return HeartRate(avg = samples.map { it.second }.average().toInt(), max = samples.maxOf { it.second }.toInt(),
            min = samples.minOf { it.second }.toInt(), pts = pts, fetchedAt = now, forEnd = end)
    }
}

// ============================ POST-WORKOUT CARD ============================
private val HrRed = Color(0xFFEF2B36)

@Composable
fun HeartRateCard(vm: OverloadViewModel, key: String, s: Session) {
    LaunchedEffect(key, s.end) { vm.loadHeartRate(key) }
    val hr = s.hr
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvText("❤️ HEART RATE", 13, FontWeight.ExtraBold, ov.mute, Modifier.weight(1f))
            OvButton("↻", { vm.loadHeartRate(key, force = true) }, small = true)
        }
        when {
            hr == null -> Sub("Loading heart rate…", Modifier.padding(top = 8.dp))
            hr.avg == 0 -> Sub("No heart rate for this workout yet. Samsung Health syncs to Health Connect every so often, so check back in a few minutes. " +
                "For better data, start a Weights workout on your watch too.", Modifier.padding(top = 8.dp))
            else -> {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HrTile("${hr.avg}", "Avg bpm", Modifier.weight(1f)); HrTile("${hr.max}", "Max bpm", Modifier.weight(1f))
                }
                if (hr.pts.size >= 2) {
                    Spacer(Modifier.height(12.dp))
                    HrChart(hr, ((s.end ?: 0) - (s.start ?: 0)) / 1000, Modifier.fillMaxWidth().height(150.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Sub("0 min", Modifier.weight(1f)); Sub("- - avg ${hr.avg}", color = HrRed); Spacer(Modifier.weight(1f))
                        Sub("${((s.end ?: 0) - (s.start ?: 0)) / 60_000} min")
                    }
                } else Sub("Only one reading during this workout, so there's no graph.", Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun HrTile(v: String, label: String, modifier: Modifier) =
    Column(modifier.background(ov.soft, RoundedCornerShape(16.dp)).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Title(v, 26, HrRed); Sub(label)
    }

/** Heart rate over the workout with a dashed line at the average. x = seconds from start. */
@Composable
private fun HrChart(hr: HeartRate, durSec: Long, modifier: Modifier) {
    val grid = ov.line; val mute = ov.mute
    Row(modifier) {
        Column(Modifier.fillMaxHeight().padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Sub("${hr.max}", color = mute); Sub("${hr.min}", color = mute)
        }
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            val pad = 6.dp.toPx()
            val lo = hr.min - 3f; val hi = hr.max + 3f
            val total = maxOf(durSec.toFloat(), hr.pts.last().t.toFloat(), 1f)
            fun X(t: Int) = t / total * size.width
            fun Y(b: Float) = pad + (1 - (b - lo) / (hi - lo)) * (size.height - 2 * pad)
            drawLine(grid, Offset(0f, Y(hr.max.toFloat())), Offset(size.width, Y(hr.max.toFloat())), 2f)
            drawLine(grid, Offset(0f, Y(hr.min.toFloat())), Offset(size.width, Y(hr.min.toFloat())), 2f)
            val line = Path(); hr.pts.forEachIndexed { i, p -> if (i == 0) line.moveTo(X(p.t), Y(p.b.toFloat())) else line.lineTo(X(p.t), Y(p.b.toFloat())) }
            val area = Path().apply { addPath(line); lineTo(X(hr.pts.last().t), size.height); lineTo(X(hr.pts.first().t), size.height); close() }
            drawPath(area, Brush.verticalGradient(listOf(HrRed.copy(.28f), Color.Transparent)))
            drawPath(line, HrRed, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val ay = Y(hr.avg.toFloat())
            drawLine(HrRed.copy(.75f), Offset(0f, ay), Offset(size.width, ay), 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
        }
    }
}

// ============================ PERMISSION RATIONALE ============================
/** Health Connect opens this when you tap "privacy policy" / "why does this app need access". */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OverloadTheme("auto") {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp)) {
                    Title("Heart rate in Overload", 28)
                    Spacer(Modifier.height(12.dp))
                    OvText("Overload reads your heart rate from Health Connect only for the time between starting and finishing a workout. " +
                        "It uses it to show your average and max heart rate and a heart rate graph after the workout.", 16)
                    Spacer(Modifier.height(10.dp))
                    OvText("Your heart rate is stored only on this phone, inside Overload's own data and any backup file you save. " +
                        "Overload never writes to Health Connect and never sends your health data anywhere.", 16)
                    Spacer(Modifier.height(10.dp))
                    OvText("You can turn this off any time in Overload's Settings, or remove access in Health Connect.", 16)
                    Spacer(Modifier.height(20.dp))
                    OvButton("Close", { finish() }, bg = ov.ink, fg = ov.bg)
                }
            }
        }
    }
}
