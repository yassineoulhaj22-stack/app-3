package com.zonerush.game

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var coinsTv: TextView
    private lateinit var nameEt: EditText

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        val pad = (24 * d).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setPadding(pad, pad, pad, pad)

        val logo = ImageView(this)
        logo.setImageResource(R.drawable.ic_logo_fg)
        root.addView(logo, LinearLayout.LayoutParams((160 * d).toInt(), (160 * d).toInt()))

        val title = TextView(this)
        title.text = "ZONE RUSH"
        title.textSize = 34f
        title.setTextColor(0xFFFF6D00.toInt())
        title.gravity = Gravity.CENTER
        root.addView(title)

        val sub = TextView(this)
        sub.text = "Battle royale Sahara • 100% hors-ligne"
        sub.setTextColor(Color.WHITE)
        sub.gravity = Gravity.CENTER
        root.addView(sub)

        coinsTv = TextView(this)
        coinsTv.textSize = 20f
        coinsTv.setTextColor(0xFFFFD600.toInt())
        coinsTv.gravity = Gravity.CENTER
        root.addView(coinsTv, lp(d, 12))

        nameEt = EditText(this)
        nameEt.hint = "Ton pseudo"
        nameEt.setText(Store.name(this))
        nameEt.setTextColor(Color.WHITE)
        nameEt.setHintTextColor(0xFF888888.toInt())
        nameEt.maxLines = 1
        root.addView(nameEt, lp(d, 8))

        addBtn(root, d, "▶  Jouer solo (contre des bots)") { start("solo", "") }
        addBtn(root, d, "📡  Créer un salon (Wi-Fi / Bluetooth)") {
            start("host", (1000..9999).random().toString())
        }
        addBtn(root, d, "🔑  Rejoindre avec un code") { askCode() }
        addBtn(root, d, "👕  Boutique : couleurs et chapeaux") {
            startActivity(Intent(this, ShopActivity::class.java))
        }

        val info = TextView(this)
        info.text = "Multijoueur sans internet : les téléphones doivent être proches " +
            "(même pièce, ~30 m). Active Bluetooth, Wi-Fi et la localisation. " +
            "Maintiens 🎤 pour parler, 😀 pour danser (danse de groupe = bonus)."
        info.setTextColor(0xFFAAAAAA.toInt())
        info.textSize = 12f
        root.addView(info, lp(d, 16))

        val sv = ScrollView(this)
        sv.setBackgroundColor(0xFF101820.toInt())
        sv.addView(root)
        setContentView(sv)

        val missing = perms().filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 1)
    }

    override fun onResume() {
        super.onResume()
        coinsTv.text = "🪙 ${Store.coins(this)}"
    }

    private fun lp(d: Float, top: Int) =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = (top * d).toInt() }

    private fun addBtn(root: LinearLayout, d: Float, label: String, f: () -> Unit) {
        val btn = Button(this)
        btn.text = label
        btn.setOnClickListener { f() }
        root.addView(btn, lp(d, 8))
    }

    private fun perms(): List<String> {
        val l = ArrayList<String>()
        l.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31) {
            l.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            l.add(Manifest.permission.BLUETOOTH_CONNECT)
            l.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT <= 32) l.add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) l.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        return l
    }

    private fun askCode() {
        val et = EditText(this)
        et.inputType = InputType.TYPE_CLASS_NUMBER
        et.hint = "Code à 4 chiffres"
        AlertDialog.Builder(this)
            .setTitle("Code du salon")
            .setView(et)
            .setPositiveButton("Rejoindre") { _, _ ->
                val c = et.text.toString().trim()
                if (c.length == 4) start("join", c)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun start(mode: String, code: String) {
        Store.setName(this, nameEt.text.toString().trim().ifBlank { "Joueur" })
        startActivity(
            Intent(this, GameActivity::class.java).putExtra("mode", mode).putExtra("code", code)
        )
    }
}
