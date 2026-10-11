package com.example.timerecorder

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF1E1E1E)
private val Black = Color.Black
private val Digits = Color(0xFFDADADA)
private val Muted = Color(0xFF999999)
private val Red = Color(0xFFB00000)
private val Card = Color(0xFF292929)

private data class Session(val duration: Long, val timestamp: Long, val subject: String, val notes: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RecorderApp() }
    }
}

@Composable
private fun RecorderApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("study_recorder", android.content.Context.MODE_PRIVATE) }
    var saved by remember { mutableStateOf(readSessions(prefs.getString("sessions", "[]") ?: "[]")) }
    var running by remember { mutableStateOf(false) }
    var carried by remember { mutableLongStateOf(0L) }
    var segmentStart by remember { mutableLongStateOf(0L) }
    var wallStart by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var page by remember { mutableIntStateOf(0) }
    var showSave by remember { mutableStateOf(false) }
    var loggedMs by remember { mutableLongStateOf(0L) }
    var loggedAt by remember { mutableLongStateOf(0L) }
    var subject by remember { mutableStateOf("Maths 1") }
    var minutes by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            elapsed = carried + SystemClock.elapsedRealtime() - segmentStart
            delay(40)
        }
    }
    fun persist() { prefs.edit().putString("sessions", writeSessions(saved)).apply() }
    fun start() {
        if (!running) {
            if (carried == 0L) wallStart = System.currentTimeMillis()
            segmentStart = SystemClock.elapsedRealtime()
            running = true
        }
    }
    fun pause() {
        if (running) {
            carried += SystemClock.elapsedRealtime() - segmentStart
            elapsed = carried
            running = false
        }
    }
    fun clearTimer() {
        running = false
        carried = 0L
        elapsed = 0L
        segmentStart = 0L
        wallStart = 0L
    }
    fun finish() {
        loggedMs = carried + if (running) SystemClock.elapsedRealtime() - segmentStart else 0L
        running = false
        elapsed = loggedMs
        loggedAt = if (wallStart > 0L) wallStart else System.currentTimeMillis()
        minutes = String.format(Locale.US, "%.2f", loggedMs / 60000.0)
        subject = "Maths 1"
        notes = ""
        validation = false
        if (loggedMs > 0L) showSave = true else clearTimer()
    }
    fun save() {
        val value = minutes.toDoubleOrNull()
        if (value == null || value <= 0.0 || subject.isBlank()) {
            validation = true
            return
        }
        saved = (listOf(Session((value * 60000.0).toLong(), loggedAt, subject.trim(), notes.trim())) + saved)
        persist()
        showSave = false
        validation = false
        clearTimer()
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Bg, surface = Card, onSurface = Color.White)) {
        Column(
            Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("STUDY TIME RECORDER", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
            Text("Track the hours. Improve the process.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Tab("STOPWATCH", page == 0) { page = 0 }
                Tab("ANALYTICS", page == 1) { page = 1 }
                Tab("HISTORY", page == 2) { page = 2 }
            }
            Spacer(Modifier.height(22.dp))

            when (page) {
                0 -> {
                    Column(
                        Modifier.fillMaxWidth().background(Black, RoundedCornerShape(26.dp)).padding(horizontal = 10.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("CURRENT STUDY SESSION", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(22.dp))
                        DigitalDisplay(elapsed, Modifier.fillMaxWidth().height(100.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(if (running) "FOCUS IN PROGRESS" else if (elapsed > 0L) "PAUSED" else "READY TO FOCUS",
                            color = if (running) Color(0xFFFF6464) else Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(22.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { start() }, enabled = !running, shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Red, disabledContainerColor = Color(0xFF601010)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                            ) { Text(if (elapsed > 0L) "Resume" else "Start", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { pause() }, enabled = running, shape = CircleShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White, disabledContentColor = Color(0xFF777777)),
                                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 14.dp)
                            ) { Text("Pause", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = { finish() }, shape = CircleShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)
                            ) { Text("Finish & Save", fontSize = 11.sp) }
                        }
                        Text("Pause keeps the time. Finish & Save lets you confirm the log.", color = Color(0xFF777777), fontSize = 10.sp,
                            modifier = Modifier.padding(top = 16.dp))
                    }
                    Spacer(Modifier.height(22.dp))
                    val today = saved.filter { sameDay(it.timestamp, System.currentTimeMillis()) }
                    val totals = today.groupBy { it.subject }.mapValues { entry -> entry.value.sumOf { it.duration } }
                    Section("TODAY'S PROGRESS")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric("STUDIED TODAY", human(today.sumOf { it.duration }), Modifier.weight(1f))
                        Metric("SESSIONS", today.size.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Metric("TOP SUBJECT TODAY", totals.maxByOrNull { it.value }?.key ?: "No sessions yet", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(20.dp))
                    Section("RECENT SESSIONS")
                    if (saved.isEmpty()) EmptyState() else saved.take(3).forEach { SessionItem(it) }
                }
                1 -> {
                    val today = saved.filter { sameDay(it.timestamp, System.currentTimeMillis()) }
                    val week = saved.filter { it.timestamp >= dayStart(System.currentTimeMillis()) - 6L * 86400000L }
                    val weeklyMs = week.sumOf { it.duration }
                    Section("STUDY PERFORMANCE")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric("TODAY", human(today.sumOf { it.duration }), Modifier.weight(1f))
                        Metric("LAST 7 DAYS", human(weeklyMs), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric("DAILY AVERAGE", human(weeklyMs / 7L), Modifier.weight(1f))
                        Metric("WEEKLY SESSIONS", week.size.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(20.dp))
                    Section("TIME BY SUBJECT")
                    val subjects = saved.groupBy { it.subject }.mapValues { e -> e.value.sumOf { it.duration } }.toList().sortedByDescending { it.second }
                    if (subjects.isEmpty()) EmptyState() else subjects.forEach { entry ->
                        Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(14.dp)).padding(13.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.first, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                Text(human(entry.second), color = Digits, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            val fraction = entry.second.toFloat() / subjects.sumOf { it.second }.coerceAtLeast(1L).toFloat()
                            Box(Modifier.fillMaxWidth().height(5.dp).background(Color(0xFF444444), CircleShape)) {
                                Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(Red, CircleShape))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("These metrics reflect the sessions you record. The app cannot measure concentration itself.", color = Muted, fontSize = 11.sp)
                }
                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SESSION HISTORY", color = Color.White, fontSize = 12.sp, letterSpacing = 1.3.sp, modifier = Modifier.weight(1f))
                        if (saved.isNotEmpty()) TextButton(onClick = { saved = emptyList(); persist() }) { Text("Clear all", color = Color(0xFFE36A6A)) }
                    }
                    if (saved.isEmpty()) EmptyState() else saved.forEach { SessionItem(it) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("YOUR STUDY DATA STAYS ON THIS DEVICE", color = Color(0xFF666666), fontSize = 9.sp, letterSpacing = 1.sp)
        }

        if (showSave) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Log study time") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Confirm the subject and edit the amount of time to record.", color = Muted, fontSize = 12.sp)
                        OutlinedTextField(subject, { subject = it; validation = false }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            minutes, { v -> minutes = v.filter { it.isDigit() || it == '.' }.take(9); validation = false },
                            label = { Text("Time to log (minutes)") },
                            supportingText = { Text("Timer measured " + formatDuration(loggedMs) + ". Adjust if needed.") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(notes, { notes = it }, label = { Text("Topic / notes (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        if (validation) Text("Enter a subject and duration greater than zero.", color = Color(0xFFFF7777), fontSize = 11.sp)
                    }
                },
                confirmButton = { TextButton(onClick = { save() }) { Text("Save session", color = Red) } },
                dismissButton = { TextButton(onClick = { showSave = false; clearTimer() }) { Text("Discard", color = Muted) } },
                containerColor = Card,
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

@Composable
private fun Tab(label: String, active: Boolean, click: () -> Unit) {
    TextButton(
        onClick = click, shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(containerColor = if (active) Color(0xFF383838) else Color.Transparent,
            contentColor = if (active) Color.White else Muted),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, letterSpacing = 0.6.sp) }
}
@Composable private fun Section(text: String) {
    Text(text, color = Color.White, fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
}
@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Card, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 0.6.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable private fun SessionItem(item: Session) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(Card, RoundedCornerShape(14.dp)).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(Red, CircleShape))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(item.subject, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(dateText(item.timestamp), color = Muted, fontSize = 10.sp)
            if (item.notes.isNotBlank()) Text(item.notes, color = Color.LightGray, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(human(item.duration), color = Digits, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
@Composable private fun EmptyState() {
    Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(14.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No study sessions yet", color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(5.dp))
        Text("Start the stopwatch, then finish and save a session.", color = Muted, fontSize = 10.sp)
    }
}
@Composable private fun DigitalDisplay(ms: Long, modifier: Modifier = Modifier) {
    val cs = ms.coerceAtLeast(0L) / 10L
    val display = "%02d:%02d:%02d".format(Locale.US, (cs / 6000L) % 100, (cs / 100L) % 60, cs % 100)
    Canvas(modifier) {
        val colonW = size.width * 0.035f
        val gap = size.width * 0.012f
        val digitW = (size.width - 2f * colonW - 7f * gap) / 6f
        var x = 0f
        display.forEach { ch ->
            if (ch == ':') {
                val r = size.height * 0.035f
                drawCircle(Digits, r, Offset(x + colonW / 2f, size.height * 0.36f))
                drawCircle(Digits, r, Offset(x + colonW / 2f, size.height * 0.68f))
                x += colonW + gap
            } else {
                drawNumber(ch.digitToInt(), x, digitW, size.height)
                x += digitW + gap
            }
        }
    }
}
private fun DrawScope.drawNumber(n: Int, x: Float, w: Float, h: Float) {
    val active = arrayOf("abcdef", "bc", "abged", "abgcd", "fgbc", "afgcd", "afgecd", "abc", "abcdefg", "abfgcd")[n]
    val t = w * .17f
    val b = t * .55f
    val mid = h / 2f
    fun hz(left: Float, top: Float, width: Float) = Path().apply {
        moveTo(left + b, top); lineTo(left + width - b, top); lineTo(left + width, top + t / 2)
        lineTo(left + width - b, top + t); lineTo(left + b, top + t); lineTo(left, top + t / 2); close()
    }
    fun vt(left: Float, top: Float, height: Float) = Path().apply {
        moveTo(left, top + b); lineTo(left + t / 2, top); lineTo(left + t, top + b)
        lineTo(left + t, top + height - b); lineTo(left + t / 2, top + height); lineTo(left, top + height - b); close()
    }
    val inset = t * .15f
    val half = (h - t * 1.6f) / 2f
    val paths = mapOf('a' to hz(x + inset, 0f, w - 2 * inset), 'g' to hz(x + inset, mid - t / 2, w - 2 * inset),
        'd' to hz(x + inset, h - t, w - 2 * inset), 'f' to vt(x, t * .75f, half),
        'b' to vt(x + w - t, t * .75f, half), 'e' to vt(x, mid, half), 'c' to vt(x + w - t, mid, half))
    paths.forEach { (key, path) -> drawPath(path, if (active.contains(key)) Digits else Color(0xFF252525)) }
}
private fun human(ms: Long): String {
    val total = ms.coerceAtLeast(0L) / 60000L
    val h = total / 60L
    val m = total % 60L
    return when { h > 0 -> "%dh %dm".format(h, m); total > 0 -> "%dm".format(total); ms > 0 -> "<1m"; else -> "0m" }
}
private fun formatDuration(ms: Long): String {
    val centis = ms / 10L % 100
    val seconds = ms / 1000L % 60
    val minutes = ms / 60000L % 60
    val hours = ms / 3600000L
    return if (hours > 0) "%02d:%02d:%02d.%02d".format(Locale.US, hours, minutes, seconds, centis)
    else "%02d:%02d.%02d".format(Locale.US, minutes, seconds, centis)
}
private fun dateText(time: Long) = SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault()).format(Date(time))
private fun dayStart(time: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    return c.timeInMillis
}
private fun sameDay(a: Long, b: Long) = dayStart(a) == dayStart(b)
private fun readSessions(raw: String): List<Session> = try {
    val a = JSONArray(raw)
    (0 until a.length()).map { i ->
        val o = a.getJSONObject(i)
        Session(o.getLong("duration"), o.getLong("timestamp"), o.optString("subject", "Study"), o.optString("notes", ""))
    }.sortedByDescending { it.timestamp }
} catch (_: Exception) { emptyList() }
private fun writeSessions(items: List<Session>): String {
    val a = JSONArray()
    items.forEach { s -> a.put(JSONObject().put("duration", s.duration).put("timestamp", s.timestamp).put("subject", s.subject).put("notes", s.notes)) }
    return a.toString()
}
