package com.zonerush.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Arene battle-royale 2D originale: zone qui retrecit, loot, bots, multijoueur local, emotes. */
class GameView(ctx: Context) : View(ctx) {

    class P(val id: String) {
        var x = 0f; var y = 0f; var tx = 0f; var ty = 0f
        var angle = 0f; var hp = 100f
        var body = 0; var hat = 0; var name = ""
        var emote = -1; var emoteT = 0f
        var bot = false; var alive = true
        var seen = 0f; var cd = 0f; var rifle = false
        var bt = 0f; var dirx = 0f; var diry = 0f
    }

    class Bu(var x: Float, var y: Float, val vx: Float, val vy: Float, val owner: String, var life: Float = 0.9f)
    class Loot(val x: Float, val y: Float, val type: Int, var on: Boolean = true)

    var net: Net? = null
    var solo = true
    var onEnd: ((Boolean, Int, Int) -> Unit)? = null

    private val world = 3000f
    val me = P(java.util.UUID.randomUUID().toString().take(6))
    private val players = HashMap<String, P>()
    private val bullets = ArrayList<Bu>()
    private val loots = ArrayList<Loot>()
    private val walls = ArrayList<RectF>()

    private var started = false
    private var ended = false
    private var t = 0f
    private var acc = 0f
    private var kills = 0
    private var bonus = 0
    private var zoneR = 1500f
    private var groupN = 0
    private var danceT = 0f
    private var last = System.nanoTime()

    // joysticks
    private var moveId = -1; private var aimId = -1
    private var mcx = 0f; private var mcy = 0f; private var mkx = 0f; private var mky = 0f
    private var acx = 0f; private var acy = 0f; private var akx = 0f; private var aky = 0f
    private var mvx = 0f; private var mvy = 0f; private var ax = 0f; private var ay = 0f

    private val pt = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tc = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE }
    private val tl = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT; color = Color.WHITE }
    private val pa = Path()
    private val tr = RectF()

    // ---------------------------------------------------------------- start
    fun startGame(seed: Int, bots: Int) {
        val rnd = Random(seed)
        walls.clear(); loots.clear(); bullets.clear(); players.clear()
        repeat(70) {
            val w = 60f + rnd.nextFloat() * 160f
            val h = 60f + rnd.nextFloat() * 160f
            val x = 100f + rnd.nextFloat() * (world - 300f)
            val y = 100f + rnd.nextFloat() * (world - 300f)
            walls.add(RectF(x, y, x + w, y + h))
        }
        repeat(40) {
            loots.add(
                Loot(
                    100f + rnd.nextFloat() * (world - 200f),
                    100f + rnd.nextFloat() * (world - 200f),
                    if (rnd.nextInt(4) == 0) 1 else 0
                )
            )
        }
        me.hp = 100f; me.alive = true; me.rifle = false; me.emote = -1; me.cd = 0f
        me.body = Store.body(context); me.hat = Store.hat(context); me.name = Store.name(context)
        spot(me)
        players[me.id] = me
        for (i in 0 until bots) {
            val b = P("b$i")
            b.bot = true; b.name = "Bot ${i + 1}"
            b.body = Random.nextInt(6); b.hat = Random.nextInt(5)
            spot(b)
            players[b.id] = b
        }
        t = 0f; acc = 0f; kills = 0; bonus = 0; groupN = 0; danceT = 0f
        ended = false; started = true
        last = System.nanoTime()
        invalidate()
    }

    private fun spot(p: P) {
        var tries = 0
        do {
            val a = Random.nextFloat() * 6.2832f
            val r = 300f + Random.nextFloat() * 1000f
            p.x = world / 2 + cos(a) * r
            p.y = world / 2 + sin(a) * r
            tries++
        } while (hitsWall(p.x, p.y, 24f) && tries < 50)
        p.tx = p.x; p.ty = p.y
    }

    fun doEmote(i: Int) {
        if (!started || ended || !me.alive) return
        me.emote = i
        me.emoteT = 4f
    }

    // ---------------------------------------------------------------- network
    fun onNet(m: String) {
        if (!started) return
        val s = m.split("|")
        try {
            when (s[0]) {
                "P" -> {
                    val id = s[1]
                    if (id == me.id) return
                    val p = players.getOrPut(id) {
                        P(id).also { it.x = s[2].toFloat(); it.y = s[3].toFloat() }
                    }
                    p.tx = s[2].toFloat(); p.ty = s[3].toFloat()
                    p.angle = s[4].toFloat(); p.hp = s[5].toFloat()
                    p.body = s[6].toInt(); p.hat = s[7].toInt()
                    p.emote = s[8].toInt(); p.name = s[9]
                    p.alive = p.hp > 0f
                    p.seen = 0f
                }
                "F" -> {
                    val a = s[4].toFloat()
                    bullets.add(Bu(s[2].toFloat(), s[3].toFloat(), cos(a) * 720f, sin(a) * 720f, s[1]))
                }
                "D" -> {
                    players[s[1]]?.let { it.alive = false; it.hp = 0f }
                    if (s[2] == me.id) kills++
                }
                "L" -> loots.getOrNull(s[1].toInt())?.on = false
            }
        } catch (e: Exception) {
        }
    }

    // ---------------------------------------------------------------- logic
    private fun hitsWall(x: Float, y: Float, r: Float): Boolean {
        for (w in walls) {
            val cx = x.coerceIn(w.left, w.right)
            val cy = y.coerceIn(w.top, w.bottom)
            if ((x - cx) * (x - cx) + (y - cy) * (y - cy) < r * r) return true
        }
        return false
    }

    private fun moveEntity(p: P, dx: Float, dy: Float) {
        val ox = p.x
        p.x = (p.x + dx).coerceIn(20f, world - 20f)
        if (hitsWall(p.x, p.y, 16f)) p.x = ox
        val oy = p.y
        p.y = (p.y + dy).coerceIn(20f, world - 20f)
        if (hitsWall(p.x, p.y, 16f)) p.y = oy
    }

    private fun fire(p: P) {
        p.cd = if (p.rifle) 0.18f else 0.38f
        val a = p.angle
        val sx = p.x + cos(a) * 22f
        val sy = p.y + sin(a) * 22f
        bullets.add(Bu(sx, sy, cos(a) * 720f, sin(a) * 720f, p.id))
        if (p === me) net?.sendText("F|${me.id}|$sx|$sy|$a")
    }

    private fun damage(p: P, d: Float, by: String) {
        if (!p.alive) return
        p.hp -= d
        if (p.hp <= 0f) {
            p.hp = 0f
            p.alive = false
            if (p === me) net?.sendText("D|${me.id}|$by")
            if (by == me.id) kills++
        }
    }

    private fun end(win: Boolean) {
        if (ended) return
        ended = true
        val coins = kills * 20 + bonus + (if (win) 100 else 10) + (t / 10f).toInt()
        Store.addCoins(context, coins)
        onEnd?.invoke(win, kills, coins)
    }

    private fun update(dt: Float) {
        if (!started || ended) return
        t += dt
        zoneR = max(120f, 1500f - t * 11f)

        if (me.alive) {
            moveEntity(me, mvx * 230f * dt, mvy * 230f * dt)
            val al = hypot(ax, ay)
            if (al > 0.2f) me.angle = atan2(ay, ax)
            me.cd -= dt
            if (al > 0.5f && me.cd <= 0f) fire(me)
            if (me.emoteT > 0f) {
                me.emoteT -= dt
                if (me.emoteT <= 0f) me.emote = -1
            }
            if (me.emote >= 0) {
                var n = 0
                for (o in players.values) {
                    if (o !== me && o.alive && o.emote == me.emote && hypot(o.x - me.x, o.y - me.y) < 220f) n++
                }
                groupN = n
                if (n > 0) {
                    danceT += dt
                    if (danceT >= 5f) { danceT = 0f; bonus += 5 * n }
                } else danceT = 0f
            } else groupN = 0
        }

        // bots
        for (b in players.values) {
            if (!b.bot || !b.alive) continue
            b.cd -= dt; b.bt -= dt
            var tg: P? = null
            var bd = 1e9f
            for (o in players.values) {
                if (o === b || !o.alive) continue
                val d = hypot(o.x - b.x, o.y - b.y)
                if (d < bd) { bd = d; tg = o }
            }
            val target = tg
            if (b.bt <= 0f) {
                b.bt = 0.6f + Random.nextFloat() * 0.8f
                val dz = hypot(b.x - world / 2, b.y - world / 2)
                if (dz > zoneR - 80f) {
                    val a = atan2(world / 2 - b.y, world / 2 - b.x)
                    b.dirx = cos(a); b.diry = sin(a)
                } else if (target != null) {
                    val a = atan2(target.y - b.y, target.x - b.x)
                    val s = if (bd > 260f) 1f else -0.3f
                    val side = if (Random.nextBoolean()) 1f else -1f
                    b.dirx = cos(a) * s - sin(a) * side * 0.6f
                    b.diry = sin(a) * s + cos(a) * side * 0.6f
                }
            }
            moveEntity(b, b.dirx * 150f * dt, b.diry * 150f * dt)
            if (target != null && bd < 520f) {
                b.angle = atan2(target.y - b.y, target.x - b.x) + (Random.nextFloat() - 0.5f) * 0.35f
                if (b.cd <= 0f) {
                    fire(b)
                    b.cd = 0.8f + Random.nextFloat() * 0.5f
                }
            }
        }

        // remote players smoothing
        for (o in players.values) {
            if (o.bot || o === me) continue
            o.x += (o.tx - o.x) * min(1f, dt * 12f)
            o.y += (o.ty - o.y) * min(1f, dt * 12f)
            o.seen += dt
        }
        if (!solo) players.values.removeAll { !it.bot && it !== me && it.seen > 6f }

        // bullets
        val iter = bullets.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt
            var dead = b.life <= 0f || hitsWall(b.x, b.y, 3f)
            if (!dead) {
                for (p in players.values) {
                    if (!p.alive || p.id == b.owner) continue
                    if (hypot(p.x - b.x, p.y - b.y) < 18f) {
                        dead = true
                        if (solo || p === me) damage(p, 12f, b.owner)
                        break
                    }
                }
            }
            if (dead) iter.remove()
        }

        // zone damage + loot
        for (p in players.values) {
            if (!p.alive) continue
            if (!solo && p !== me) continue
            if (hypot(p.x - world / 2, p.y - world / 2) > zoneR) damage(p, 4f * dt, "zone")
            for (i in loots.indices) {
                val l = loots[i]
                if (!l.on) continue
                if (hypot(p.x - l.x, p.y - l.y) < 30f) {
                    if (l.type == 0) p.hp = min(100f, p.hp + 35f) else p.rifle = true
                    l.on = false
                    if (p === me) net?.sendText("L|$i")
                }
            }
        }

        // network state
        val n = net
        if (n != null) {
            acc += dt
            if (acc >= 0.1f) {
                acc = 0f
                n.sendText(
                    "P|${me.id}|${me.x}|${me.y}|${me.angle}|${me.hp}|${me.body}|${me.hat}|${me.emote}|" +
                        me.name.replace("|", "")
                )
            }
        }

        // victory / defeat
        val aliveCount = players.values.count { it.alive }
        if (!me.alive) end(false)
        else if (t > 3f && aliveCount == 1 && (players.size > 1 || !solo)) end(true)
    }

    // ---------------------------------------------------------------- draw
    private fun drawHat(c: Canvas, x: Float, y: Float, h: Int) {
        pt.style = Paint.Style.FILL
        when (h) {
            1 -> {
                pt.color = 0xFFD32F2F.toInt()
                tr.set(x - 14f, y - 28f, x + 14f, y)
                c.drawArc(tr, 180f, 180f, true, pt)
                c.drawRect(x, y - 18f, x + 20f, y - 14f, pt)
            }
            2 -> {
                pt.color = 0xFFFFD600.toInt()
                pa.reset()
                pa.moveTo(x - 12f, y - 14f); pa.lineTo(x - 12f, y - 28f); pa.lineTo(x - 6f, y - 21f)
                pa.lineTo(x, y - 30f); pa.lineTo(x + 6f, y - 21f); pa.lineTo(x + 12f, y - 28f)
                pa.lineTo(x + 12f, y - 14f); pa.close()
                c.drawPath(pa, pt)
            }
            3 -> {
                pt.color = 0xFFEEEEEE.toInt()
                pa.reset()
                pa.moveTo(x - 14f, y - 12f); pa.lineTo(x - 6f, y - 14f); pa.lineTo(x - 16f, y - 30f); pa.close()
                pa.moveTo(x + 14f, y - 12f); pa.lineTo(x + 6f, y - 14f); pa.lineTo(x + 16f, y - 30f); pa.close()
                c.drawPath(pa, pt)
            }
            4 -> {
                pt.color = 0xFFFFEB3B.toInt()
                pt.style = Paint.Style.STROKE
                pt.strokeWidth = 4f
                tr.set(x - 12f, y - 36f, x + 12f, y - 26f)
                c.drawOval(tr, pt)
                pt.style = Paint.Style.FILL
            }
        }
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = min(0.05f, (now - last) / 1e9f)
        last = now
        update(dt)

        c.drawColor(0xFFD2B27A.toInt())
        val d = resources.displayMetrics.density
        if (started) {
            val z = d * 0.55f
            c.save()
            c.translate(width / 2f, height / 2f)
            c.scale(z, z)
            c.translate(-me.x, -me.y)

            // grid + border
            pt.style = Paint.Style.STROKE; pt.strokeWidth = 2f; pt.color = 0x22000000
            var g = 0f
            while (g <= world) { c.drawLine(g, 0f, g, world, pt); c.drawLine(0f, g, world, g, pt); g += 150f }
            pt.strokeWidth = 10f; pt.color = 0xFFFFFFFF.toInt()
            c.drawRect(0f, 0f, world, world, pt)

            // zone
            pa.reset()
            pa.addRect(-3000f, -3000f, world + 3000f, world + 3000f, Path.Direction.CW)
            pa.addCircle(world / 2, world / 2, zoneR, Path.Direction.CCW)
            pa.fillType = Path.FillType.EVEN_ODD
            pt.style = Paint.Style.FILL; pt.color = 0x55FF0000
            c.drawPath(pa, pt)
            pt.style = Paint.Style.STROKE; pt.strokeWidth = 6f; pt.color = 0xFFFF5252.toInt()
            c.drawCircle(world / 2, world / 2, zoneR, pt)

            // walls
            for (w in walls) {
                pt.style = Paint.Style.FILL; pt.color = 0xFF8D5B3A.toInt(); c.drawRect(w, pt)
                pt.style = Paint.Style.STROKE; pt.strokeWidth = 4f; pt.color = 0xFF4E342E.toInt(); c.drawRect(w, pt)
            }

            // loot
            for (l in loots) {
                if (!l.on) continue
                pt.style = Paint.Style.FILL
                pt.color = if (l.type == 0) 0xFF00E676.toInt() else 0xFFFFC400.toInt()
                c.drawCircle(l.x, l.y, 14f, pt)
                tc.textSize = 18f
                c.drawText(if (l.type == 0) "+" else "R", l.x, l.y + 6f, tc)
            }

            // bullets
            pt.style = Paint.Style.FILL; pt.color = 0xFFFFEB3B.toInt()
            for (b in bullets) c.drawCircle(b.x, b.y, 4f, pt)

            // players
            for (pl in players.values) {
                if (!pl.alive) continue
                val bob = if (pl.emote >= 0) sin(t * 12f) * 6f else 0f
                val px = pl.x
                val py = pl.y + bob
                pt.style = Paint.Style.FILL
                pt.color = Items.bodyColors[pl.body.coerceIn(0, 5)]
                c.drawCircle(px, py, 16f, pt)
                if (pl === me) {
                    pt.style = Paint.Style.STROKE; pt.strokeWidth = 3f; pt.color = Color.WHITE
                    c.drawCircle(px, py, 18f, pt)
                }
                pt.style = Paint.Style.STROKE; pt.strokeWidth = 6f; pt.color = 0xFF212121.toInt()
                c.drawLine(px, py, px + cos(pl.angle) * 30f, py + sin(pl.angle) * 30f, pt)
                drawHat(c, px, py, pl.hat)
                tc.textSize = 16f
                c.drawText(pl.name, px, py - 44f, tc)
                pt.style = Paint.Style.FILL; pt.color = 0xFF000000.toInt()
                c.drawRect(px - 18f, py + 22f, px + 18f, py + 28f, pt)
                pt.color = 0xFF00E676.toInt()
                c.drawRect(px - 18f, py + 22f, px - 18f + 36f * (pl.hp / 100f), py + 28f, pt)
                if (pl.emote >= 0) {
                    tc.textSize = 26f
                    c.drawText(Items.emoteIcons[pl.emote.coerceIn(0, 5)], px, py - 58f, tc)
                }
            }
            c.restore()

            // HUD
            tl.textSize = 14f * d
            val alive = players.values.count { it.alive }
            c.drawText("👥 $alive   ☠ $kills   ⭕ ${zoneR.toInt()}", 12f * d, 24f * d, tl)
            pt.style = Paint.Style.FILL; pt.color = 0xFF000000.toInt()
            c.drawRect(12f * d, 34f * d, 152f * d, 44f * d, pt)
            pt.color = if (me.hp > 35f) 0xFF00E676.toInt() else 0xFFFF1744.toInt()
            c.drawRect(12f * d, 34f * d, (12f + 140f * (me.hp / 100f)) * d, 44f * d, pt)
            if (me.rifle) c.drawText("🔫 Fusil rapide", 12f * d, 64f * d, tl)
            if (groupN > 0) {
                tc.textSize = 18f * d
                c.drawText("💃 Danse de groupe x${groupN + 1} !", width / 2f, 30f * d, tc)
            }
            if (t < 8f) {
                tc.textSize = 14f * d
                c.drawText("Gauche: bouger  •  Droite: viser et tirer", width / 2f, height - 20f * d, tc)
            }

            val r = 55f * d
            pt.strokeWidth = 3f
            if (moveId >= 0) {
                pt.style = Paint.Style.STROKE; pt.color = 0x88FFFFFF.toInt()
                c.drawCircle(mcx, mcy, r, pt)
                pt.style = Paint.Style.FILL; pt.color = 0x88FFFFFF.toInt()
                c.drawCircle(mkx, mky, r * 0.4f, pt)
            }
            if (aimId >= 0) {
                pt.style = Paint.Style.STROKE; pt.color = 0x88FF6D00.toInt()
                c.drawCircle(acx, acy, r, pt)
                pt.style = Paint.Style.FILL; pt.color = 0x88FF6D00.toInt()
                c.drawCircle(akx, aky, r * 0.4f, pt)
            }
        }
        postInvalidateOnAnimation()
    }

    // ---------------------------------------------------------------- touch
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val r = 55f * resources.displayMetrics.density
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                val id = e.getPointerId(i)
                val x = e.getX(i)
                val y = e.getY(i)
                if (x < width / 2f) {
                    if (moveId < 0) { moveId = id; mcx = x; mcy = y; mkx = x; mky = y }
                } else if (aimId < 0) {
                    aimId = id; acx = x; acy = y; akx = x; aky = y
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    if (id == moveId) {
                        var dx = e.getX(i) - mcx; var dy = e.getY(i) - mcy
                        val l = hypot(dx, dy)
                        if (l > r) { dx *= r / l; dy *= r / l }
                        mkx = mcx + dx; mky = mcy + dy; mvx = dx / r; mvy = dy / r
                    } else if (id == aimId) {
                        var dx = e.getX(i) - acx; var dy = e.getY(i) - acy
                        val l = hypot(dx, dy)
                        if (l > r) { dx *= r / l; dy *= r / l }
                        akx = acx + dx; aky = acy + dy; ax = dx / r; ay = dy / r
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val id = e.getPointerId(e.actionIndex)
                if (id == moveId) { moveId = -1; mvx = 0f; mvy = 0f }
                if (id == aimId) { aimId = -1; ax = 0f; ay = 0f }
            }
            MotionEvent.ACTION_CANCEL -> {
                moveId = -1; aimId = -1; mvx = 0f; mvy = 0f; ax = 0f; ay = 0f
            }
        }
        return true
    }
}
