package com.zonerush.game

import android.content.Context

object Items {
    val bodyNames = listOf("Vert", "Bleu", "Rouge", "Violet", "Orange", "Noir")
    val bodyColors = intArrayOf(
        0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFFE53935.toInt(),
        0xFF8E24AA.toInt(), 0xFFFB8C00.toInt(), 0xFF424242.toInt()
    )
    val bodyPrices = intArrayOf(0, 50, 50, 80, 80, 120)
    val hatNames = listOf("Aucun", "Casquette", "Couronne", "Cornes", "Halo")
    val hatPrices = intArrayOf(0, 60, 200, 100, 150)
    val emotes = listOf("💃 Danse", "👋 Salut", "😂 Rire", "❤️ Coeur", "🌊 Vague", "🤖 Robot")
    val emoteIcons = listOf("💃", "👋", "😂", "❤️", "🌊", "🤖")
}

object Store {
    private fun p(c: Context) = c.getSharedPreferences("zr", Context.MODE_PRIVATE)

    fun coins(c: Context): Int = p(c).getInt("coins", 100)
    fun addCoins(c: Context, n: Int) { p(c).edit().putInt("coins", coins(c) + n).apply() }

    fun name(c: Context): String = p(c).getString("name", "Joueur") ?: "Joueur"
    fun setName(c: Context, n: String) { p(c).edit().putString("name", n.take(12)).apply() }

    fun owned(c: Context, id: String): Boolean {
        if (id == "b0" || id == "h0") return true
        val s: Set<String> = p(c).getStringSet("owned", emptySet()) ?: emptySet()
        return s.contains(id)
    }

    fun buy(c: Context, id: String, price: Int): Boolean {
        if (coins(c) < price) return false
        val old: Set<String> = p(c).getStringSet("owned", emptySet()) ?: emptySet()
        val s = HashSet<String>(old)
        s.add(id)
        p(c).edit().putStringSet("owned", s).putInt("coins", coins(c) - price).apply()
        return true
    }

    fun body(c: Context): Int = p(c).getInt("body", 0)
    fun hat(c: Context): Int = p(c).getInt("hat", 0)
    fun equipBody(c: Context, i: Int) { p(c).edit().putInt("body", i).apply() }
    fun equipHat(c: Context, i: Int) { p(c).edit().putInt("hat", i).apply() }
}
