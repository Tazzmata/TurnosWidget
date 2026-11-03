package com.ruben.turnos

import android.content.Context
import java.util.Calendar
import java.util.Locale

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("turnos", Context.MODE_PRIVATE)

    fun base(c: Context): String = sp(c).getString("base", "") ?: ""
    fun token(c: Context): String = sp(c).getString("token", "") ?: ""
    fun configurado(c: Context): Boolean = base(c).isNotBlank() && token(c).isNotBlank()

    fun guardarEnlace(c: Context, base: String, token: String) {
        sp(c).edit().putString("base", base).putString("token", token).apply()
        limpiarCache(c)
    }

    fun vista(c: Context): String = sp(c).getString("vista", "solo") ?: "solo"
    fun setVista(c: Context, v: String) {
        sp(c).edit().putString("vista", v).apply()
    }

    fun offset(c: Context): Int = sp(c).getInt("offset", 0)
    fun setOffset(c: Context, o: Int) {
        sp(c).edit().putInt("offset", o).apply()
    }

    /** Año y mes (0-11) que se está viendo, según el desplazamiento respecto al mes actual. */
    fun mes(c: Context): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, offset(c))
        return Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun claveMes(anio: Int, mes0: Int): String = String.format(Locale.ROOT, "%04d-%02d", anio, mes0 + 1)

    fun guardarCache(c: Context, clave: String, json: String) {
        sp(c).edit().putString("c_$clave", json).putLong("t_$clave", System.currentTimeMillis()).apply()
    }

    fun cache(c: Context, clave: String): String? = sp(c).getString("c_$clave", null)
    fun cacheTs(c: Context, clave: String): Long = sp(c).getLong("t_$clave", 0L)

    fun limpiarCache(c: Context) {
        val ed = sp(c).edit()
        for (k in sp(c).all.keys) {
            if (k.startsWith("c_") || k.startsWith("t_")) ed.remove(k)
        }
        ed.apply()
    }

    fun fallo(c: Context): Boolean = sp(c).getBoolean("fallo", false)
    fun setFallo(c: Context, f: Boolean) {
        sp(c).edit().putBoolean("fallo", f).apply()
    }

    fun datos(c: Context, anio: Int, mes0: Int): Datos? {
        val json = cache(c, claveMes(anio, mes0)) ?: return null
        return Modelo.parsear(json)
    }
}
