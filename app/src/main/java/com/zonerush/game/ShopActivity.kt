package com.zonerush.game

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ShopActivity : Activity() {
    private lateinit var box: LinearLayout

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val pad = (16 * resources.displayMetrics.density).toInt()
        box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, pad, pad, pad)
        val sv = ScrollView(this)
        sv.setBackgroundColor(0xFF101820.toInt())
        sv.addView(box)
        setContentView(sv)
        render()
    }

    private fun title(t: String, size: Float, color: Int) {
        val tv = TextView(this)
        tv.text = t
        tv.textSize = size
        tv.setTextColor(color)
        box.addView(tv)
    }

    private fun row(id: String, name: String, price: Int, equipped: Boolean, equip: () -> Unit) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        val tv = TextView(this)
        tv.text = name
        tv.textSize = 18f
        tv.setTextColor(Color.WHITE)
        r.addView(tv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val btn = Button(this)
        if (Store.owned(this, id)) {
            btn.text = if (equipped) "Équipé ✓" else "Équiper"
            btn.setOnClickListener { equip(); render() }
        } else {
            btn.text = "Acheter 🪙$price"
            btn.setOnClickListener {
                if (Store.buy(this, id, price)) {
                    equip()
                    render()
                } else {
                    Toast.makeText(this, "Pas assez de pièces", Toast.LENGTH_SHORT).show()
                }
            }
        }
        r.addView(btn)
        box.addView(r)
    }

    private fun render() {
        box.removeAllViews()
        title("Boutique   🪙 ${Store.coins(this)}", 24f, 0xFFFFD600.toInt())
        title("\nCouleurs", 20f, 0xFFFF6D00.toInt())
        for (i in Items.bodyNames.indices) {
            row("b$i", Items.bodyNames[i], Items.bodyPrices[i], Store.body(this) == i) { Store.equipBody(this, i) }
        }
        title("\nChapeaux", 20f, 0xFFFF6D00.toInt())
        for (i in Items.hatNames.indices) {
            row("h$i", Items.hatNames[i], Items.hatPrices[i], Store.hat(this) == i) { Store.equipHat(this, i) }
        }
        title("\nGagne des pièces : éliminations, victoires, danses de groupe.", 13f, 0xFFAAAAAA.toInt())
    }
}
