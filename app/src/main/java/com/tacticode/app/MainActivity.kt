package com.tacticode.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Design tokens (from the TactiCode design system)
// ---------------------------------------------------------------------------
val NavyBg = Color(0xFF0F172A)
val NavySurface = Color(0xFF1E293B)
val NavyRaised = Color(0xFF273449)
val NavyBorder = Color(0xFF334155)
val Neon = Color(0xFF10B981)
val TextHi = Color(0xFFF8FAFC)
val TextDim = Color(0xFF94A3B8)
val Warn = Color(0xFFF59E0B)
val Danger = Color(0xFFEF4444)
val InfoBlue = Color(0xFF38BDF8)

// 31 characters: no 0, 1, I, L, O (ambiguous glyphs removed)
const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
const val CODE_LENGTH = 6

fun generateCode(): String =
    (1..CODE_LENGTH).map { ALPHABET.random() }.joinToString("")

fun prettyCode(code: String): String =
    if (code.length == CODE_LENGTH) code.take(3) + " \u00B7 " + code.drop(3) else code

enum class Screen { HOME, JOIN, CREATE, LOBBY, MATCH }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TactiApp() }
    }
}

@Composable
fun TactiApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var code by remember { mutableStateOf("") }
    var isHost by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Neon,
            onPrimary = NavyBg,
            background = NavyBg,
            onBackground = TextHi,
            surface = NavySurface,
            onSurface = TextHi,
        )
    ) {
        BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

        Surface(modifier = Modifier.fillMaxSize(), color = NavyBg) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    onJoin = { screen = Screen.JOIN },
                    onCreate = {
                        code = generateCode()
                        isHost = true
                        screen = Screen.CREATE
                    },
                    onPractice = { screen = Screen.MATCH },
                )
                Screen.JOIN -> JoinScreen(
                    onBack = { screen = Screen.HOME },
                    onJoined = {
                        code = it
                        isHost = false
                        screen = Screen.LOBBY
                    },
                )
                Screen.CREATE -> CreateScreen(
                    code = code,
                    onRegenerate = { code = generateCode() },
                    onEnter = { screen = Screen.LOBBY },
                    onBack = { screen = Screen.HOME },
                )
                Screen.LOBBY -> LobbyScreen(
                    code = code,
                    isHost = isHost,
                    onStart = { screen = Screen.MATCH },
                    onLeave = { screen = Screen.HOME },
                )
                Screen.MATCH -> MatchScreen(onLeave = { screen = Screen.HOME })
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Reusable components
// ---------------------------------------------------------------------------
@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Neon,
            contentColor = NavyBg,
            disabledContainerColor = NavyRaised,
            disabledContentColor = TextDim,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.5.dp, Neon),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Neon),
    ) {
        Text(text, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun StatusChip(text: String, color: Color = Neon) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(NavySurface)
            .border(1.dp, NavyBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(text, color = TextHi, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------------------
// Home
// ---------------------------------------------------------------------------
@Composable
fun HomeScreen(onJoin: () -> Unit, onCreate: () -> Unit, onPractice: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.padding(top = 40.dp)) {
            StatusChip("Offline-ready")
            Spacer(Modifier.height(24.dp))
            Text("TactiCode", color = Neon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Join your squad\nwith a code.",
                color = TextHi,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "No queue, no account. Light on data, built for weak connections.",
                color = TextDim,
                fontSize = 15.sp,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("JOIN ROOM", onClick = onJoin)
            SecondaryButton("CREATE ROOM", onClick = onCreate)
            TextButton(onClick = onPractice, modifier = Modifier.fillMaxWidth()) {
                Text("Practice offline", color = TextDim)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Join by code
// ---------------------------------------------------------------------------
enum class JoinState { IDLE, SEARCHING, ERROR }

@Composable
fun JoinScreen(onBack: () -> Unit, onJoined: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(JoinState.IDLE) }
    var attempts by remember { mutableIntStateOf(0) }
    val locked = attempts >= 5
    val clipboard = LocalClipboardManager.current

    // Prototype behaviour: any code is accepted, except AAAAAA which
    // demonstrates the "room not found" error state.
    LaunchedEffect(code) {
        if (code.length == CODE_LENGTH && state == JoinState.IDLE) {
            state = JoinState.SEARCHING
            delay(900)
            if (code == "AAAAAA") {
                attempts += 1
                state = JoinState.ERROR
            } else {
                onJoined(code)
            }
        }
    }

    fun press(c: Char) {
        if (state == JoinState.SEARCHING || locked) return
        if (state == JoinState.ERROR) {
            state = JoinState.IDLE
            code = ""
        }
        if (code.length < CODE_LENGTH) code += c
    }

    fun back() {
        if (state == JoinState.SEARCHING) return
        state = JoinState.IDLE
        code = code.dropLast(1)
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = onBack) { Text("\u2190 Back", color = TextDim) }
        Spacer(Modifier.height(8.dp))
        Text("Enter room code", color = TextHi, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Ask the host for the 6-character code.", color = TextDim, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(CODE_LENGTH) { i ->
                val ch = code.getOrNull(i)?.toString() ?: ""
                val active = i == code.length && state == JoinState.IDLE
                val borderColor = when {
                    state == JoinState.ERROR -> Danger
                    state == JoinState.SEARCHING -> Neon
                    active -> Neon
                    else -> NavyBorder
                }
                val shape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(shape)
                        .background(NavySurface)
                        .border(if (active || state != JoinState.IDLE) 2.dp else 1.dp, borderColor, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ch,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state == JoinState.ERROR) Danger else TextHi,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        val (msg, msgColor) = when {
            locked -> "Too many attempts. Wait 60 seconds, then try again." to Danger
            state == JoinState.SEARCHING -> "Searching nearby, then online\u2026" to Neon
            state == JoinState.ERROR -> "Room not found. Check the code and try again." to Danger
            else -> "Tip: paste a code you received on WhatsApp." to TextDim
        }
        Text(msg, color = msgColor, fontSize = 13.sp, modifier = Modifier.height(20.dp))

        TextButton(onClick = {
            val t = clipboard.getText()?.text.orEmpty().uppercase().filter { it in ALPHABET }.take(CODE_LENGTH)
            if (t.isNotEmpty() && state != JoinState.SEARCHING && !locked) {
                state = JoinState.IDLE
                code = t
            }
        }) { Text("Paste code", color = Neon) }

        Spacer(Modifier.weight(1f))

        // Custom keypad (ambiguous characters removed)
        val rows = ALPHABET.chunked(8)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEachIndexed { r, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { ch -> Key(ch.toString(), Modifier.weight(1f)) { press(ch) } }
                    if (r == rows.lastIndex) {
                        repeat(8 - row.size - 1) { Spacer(Modifier.weight(1f)) }
                        Key("\u232B", Modifier.weight(1f)) { back() }
                    }
                }
            }
        }
    }
}

@Composable
fun Key(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(NavyRaised)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = TextHi, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

// ---------------------------------------------------------------------------
// Create room
// ---------------------------------------------------------------------------
@Composable
fun CreateScreen(code: String, onRegenerate: () -> Unit, onEnter: () -> Unit, onBack: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            TextButton(onClick = onBack) { Text("\u2190 Back", color = TextDim) }
            Spacer(Modifier.height(8.dp))
            Text("Your room is ready", color = TextHi, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Share this code with your squad.", color = TextDim, fontSize = 14.sp)
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavySurface)
                    .border(2.dp, Neon, RoundedCornerShape(16.dp))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    prettyCode(code),
                    color = Neon,
                    fontSize = 40.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    SecondaryButton("COPY") { clipboard.setText(AnnotatedString(code)) }
                }
                Box(Modifier.weight(1f)) {
                    SecondaryButton("SHARE") {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Join my TactiCode room: $code")
                        }
                        ctx.startActivity(Intent.createChooser(send, "Share room code"))
                    }
                }
            }
            TextButton(onClick = onRegenerate) { Text("Generate a new code", color = TextDim) }
        }
        PrimaryButton("ENTER LOBBY", onClick = onEnter)
    }
}

// ---------------------------------------------------------------------------
// Lobby (simulated players: replace with the real network layer)
// ---------------------------------------------------------------------------
data class Player(val name: String, val ready: Boolean, val ping: Int)

@Composable
fun LobbyScreen(code: String, isHost: Boolean, onStart: () -> Unit, onLeave: () -> Unit) {
    val players = remember {
        mutableStateListOf(Player(if (isHost) "You (Host)" else "You", false, 24))
    }

    LaunchedEffect(Unit) {
        delay(1000); players.add(Player("Amine", false, 38))
        delay(900); players.add(Player("Sara", false, 61))
        delay(900); players.add(Player("Yassine", false, 142))
        delay(1200)
        for (i in 1 until players.size) {
            players[i] = players[i].copy(ready = true)
            delay(500)
        }
    }

    val allReady = players.size >= 2 && players.all { it.ready }
    val me = players[0]

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Room code", color = TextDim, fontSize = 12.sp)
                Text(
                    prettyCode(code).ifEmpty { "------" },
                    color = Neon,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            }
            TextButton(onClick = onLeave) { Text("Leave", color = Danger) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("${players.size}/8 players")
            StatusChip("Data saver", color = InfoBlue)
        }
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Squad Alpha", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            for (i in 0 until 4) SlotCard(players.getOrNull(i))
            Spacer(Modifier.height(4.dp))
            Text("Squad Bravo", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            for (i in 4 until 8) SlotCard(players.getOrNull(i))
        }

        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (me.ready) {
                SecondaryButton("CANCEL READY") { players[0] = me.copy(ready = false) }
            } else {
                PrimaryButton("READY") { players[0] = me.copy(ready = true) }
            }
            PrimaryButton(
                if (isHost) "START MATCH" else "START MATCH (DEMO)",
                enabled = allReady,
                onClick = onStart,
            )
        }
    }
}

@Composable
fun SlotCard(p: Player?) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(NavySurface)
            .border(1.dp, if (p?.ready == true) Neon else NavyBorder, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (p == null) {
            Text("+ Open slot", color = TextDim, fontSize = 14.sp)
        } else {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(NavyRaised),
                contentAlignment = Alignment.Center,
            ) {
                Text(p.name.first().toString(), color = Neon, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text(p.name, color = TextHi, fontSize = 15.sp, modifier = Modifier.weight(1f))
            val pingColor = when {
                p.ping < 80 -> Neon
                p.ping < 150 -> Warn
                else -> Danger
            }
            Text("${p.ping} ms", color = pingColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(10.dp))
            Text(
                if (p.ready) "READY" else "WAITING",
                color = if (p.ready) Neon else TextDim,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Match HUD (static prototype of the in-game interface)
// ---------------------------------------------------------------------------
fun Modifier.plate(): Modifier =
    this.clip(RoundedCornerShape(4.dp))
        .background(NavyBg.copy(alpha = 0.88f))
        .padding(horizontal = 10.dp, vertical = 6.dp)

@Composable
fun MatchScreen(onLeave: () -> Unit) {
    var seconds by remember { mutableIntStateOf(180) }
    var kb by remember { mutableIntStateOf(0) }
    var ammo by remember { mutableIntStateOf(30) }

    LaunchedEffect(Unit) {
        while (seconds > 0) {
            delay(1000)
            seconds -= 1
            kb += 1 // ~1 KB/s target
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1220))) {
        // Placeholder "map": a simple grid
        Canvas(Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(NavyBorder.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, size.height), 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(NavyBorder.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 1f)
                y += step
            }
        }

        // Minimap (top-left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(NavyBg.copy(alpha = 0.9f))
                .border(1.dp, NavyBorder, RoundedCornerShape(8.dp)),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(Danger, radius = size.minDimension * 0.42f, center = c, style = Stroke(2f))
                drawCircle(Neon, radius = 5f, center = c)
                drawCircle(Neon, radius = 4f, center = Offset(c.x + 18f, c.y - 10f))
                drawCircle(Neon, radius = 4f, center = Offset(c.x - 14f, c.y + 16f))
                drawCircle(InfoBlue, radius = 4f, center = Offset(c.x + 28f, c.y + 22f))
            }
        }

        // Safe-zone timer (top-center)
        Text(
            "ZONE  %d:%02d".format(seconds / 60, seconds % 60),
            color = if (seconds <= 10) Warn else TextHi,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp).plate(),
        )

        // Connection + data (top-right)
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.plate(), horizontalAlignment = Alignment.End) {
                Text("24 ms", color = Neon, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("DATA $kb KB", color = TextDim, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
            TextButton(onClick = onLeave) { Text("LEAVE", color = Danger, fontWeight = FontWeight.Bold) }
        }

        // Health + joystick (bottom-left)
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.plate()) {
                Text("HP 78", color = TextHi, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(150.dp).height(8.dp).background(NavyRaised)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(0.78f).background(Neon))
                }
            }
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(NavyBg.copy(alpha = 0.5f))
                    .border(2.dp, NavyBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(NavyRaised))
            }
        }

        // Ammo + fire (bottom-right)
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "$ammo / 90",
                color = when {
                    ammo == 0 -> Danger
                    ammo <= 8 -> Warn
                    else -> TextHi
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.plate(),
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Neon)
                    .clickable { if (ammo > 0) ammo -= 1 },
                contentAlignment = Alignment.Center,
            ) {
                Text("FIRE", color = NavyBg, fontWeight = FontWeight.Bold)
            }
        }
    }
}
