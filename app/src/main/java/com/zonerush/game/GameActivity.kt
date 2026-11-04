package com.zonerush.game

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.random.Random

class GameActivity : Activity(), Net.Listener {
    private lateinit var gv: GameView
    private lateinit var lobbyBox: LinearLayout
    private lateinit var lobbyTv: TextView
    private lateinit var startBtn: Button
    private lateinit var endBox: LinearLayout
    private lateinit var endTv: TextView
    private var net: Net? = null
    private val voice = Voice()
    private var mode = "solo"
    private var code = ""
    private var begun = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)
        mode = intent.getStringExtra("mode") ?: "solo"
        code = intent.getStringExtra("code") ?: ""
        val d = resources.displayMetrics.density
        val s56 = (56 * d).toInt()

        val root = FrameLayout(this)
        gv = GameView(this)
        gv.solo = mode == "solo"
        gv.onEnd = { win, k, c -> runOnUiThread { showEnd(win, k, c) } }
        root.addView(gv)

        // bouton emote
        val emoteBtn = Button(this)
        emoteBtn.text = "😀"
        emoteBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Emotes / danses")
                .setItems(Items.emotes.toTypedArray()) { _, i -> gv.doEmote(i) }
                .show()
        }
        root.addView(
            emoteBtn,
            FrameLayout.LayoutParams(s56, s56, Gravity.TOP or Gravity.END)
                .apply { setMargins(0, (8 * d).toInt(), (8 * d).toInt(), 0) }
        )

        // bouton micro (push-to-talk)
        val mic = Button(this)
        mic.text = "🎤"
        mic.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { voice.startTalk(net); v.alpha = 0.5f }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { voice.stopTalk(); v.alpha = 1f }
            }
            true
        }
        root.addView(
            mic,
            FrameLayout.LayoutParams(s56, s56, Gravity.TOP or Gravity.END)
                .apply { setMargins(0, (8 * d).toInt(), (72 * d).toInt(), 0) }
        )

        // lobby
        lobbyBox = LinearLayout(this)
        lobbyBox.orientation = LinearLayout.VERTICAL
        lobbyBox.gravity = Gravity.CENTER
        lobbyBox.setBackgroundColor(0xDD000000.toInt())
        lobbyTv = TextView(this)
        lobbyTv.textSize = 22f
        lobbyTv.setTextColor(0xFFFFFFFF.toInt())
        lobbyTv.gravity = Gravity.CENTER
        lobbyBox.addView(lobbyTv)
        startBtn = Button(this)
        startBtn.text = "Lancer la partie"
        startBtn.isEnabled = false
        startBtn.visibility = View.GONE
        startBtn.setOnClickListener {
            val seed = Random.nextInt()
            net?.sendText("S|$seed")
            net?.stopAdvertising()
            begin(seed, 0)
        }
        lobbyBox.addView(startBtn)
        root.addView(
            lobbyBox,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        // fin de partie
        endBox = LinearLayout(this)
        endBox.orientation = LinearLayout.VERTICAL
        endBox.gravity = Gravity.CENTER
        endBox.setBackgroundColor(0xDD000000.toInt())
        endBox.visibility = View.GONE
        endTv = TextView(this)
        endTv.textSize = 26f
        endTv.setTextColor(0xFFFFFFFF.toInt())
        endTv.gravity = Gravity.CENTER
        endBox.addView(endTv)
        val back = Button(this)
        back.text = "Retour au menu"
        back.setOnClickListener { finish() }
        endBox.addView(back)
        root.addView(
            endBox,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        setContentView(root)

        if (mode == "solo") {
            mic.visibility = View.GONE
            begin(Random.nextInt(), 7)
        } else {
            val n = Net(this, this)
            net = n
            gv.net = n
            if (mode == "host") {
                lobbyTv.text = "Salon : $code\nDonne ce code aux amis proches.\nEn attente de joueurs..."
                startBtn.visibility = View.VISIBLE
                n.host(code)
            } else {
                lobbyTv.text = "Recherche du salon $code..."
                n.join(code)
            }
        }
    }

    private fun begin(seed: Int, bots: Int) {
        if (begun) return
        begun = true
        lobbyBox.visibility = View.GONE
        gv.startGame(seed, bots)
    }

    private fun showEnd(win: Boolean, kills: Int, coins: Int) {
        endTv.text = (if (win) "🏆 VICTOIRE !" else "💀 Éliminé") + "\n\n☠ Éliminations : $kills\n🪙 +$coins pièces"
        endBox.visibility = View.VISIBLE
    }

    // ---- Net.Listener
    override fun onStatus(s: String) {
        runOnUiThread {
            if (!begun) {
                lobbyTv.text = if (mode == "host") "Salon : $code\n$s" else s
            }
        }
    }

    override fun onPeers(n: Int) {
        runOnUiThread {
            if (!begun) {
                if (mode == "host") {
                    lobbyTv.text = "Salon : $code\n$n joueur(s) connecté(s)"
                    startBtn.isEnabled = n > 0
                } else if (n > 0) {
                    lobbyTv.text = "Connecté ! L'hôte va lancer la partie..."
                }
            } else if (n == 0 && mode == "join") {
                Toast.makeText(this, "Connexion à l'hôte perdue", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onText(m: String) {
        runOnUiThread {
            if (m.startsWith("S|")) {
                val seed = m.substring(2).toIntOrNull()
                if (seed != null) begin(seed, 0)
            } else {
                gv.onNet(m)
            }
        }
    }

    override fun onVoice(d: ByteArray) {
        voice.play(d)
    }

    override fun onDestroy() {
        net?.close()
        voice.release()
        super.onDestroy()
    }
}
