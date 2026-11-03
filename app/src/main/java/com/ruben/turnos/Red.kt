package com.ruben.turnos

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Red {
    /** Descarga los datos del mes (con el anterior y el siguiente) y los guarda en caché. */
    fun descargar(ctx: Context, anio: Int, mes0: Int): Boolean {
        val base = Prefs.base(ctx)
        val token = Prefs.token(ctx)
        if (base.isBlank() || token.isBlank()) return false
        val clave = Prefs.claveMes(anio, mes0)
        var con: HttpURLConnection? = null
        try {
            val url = URL(base + "?api=" + URLEncoder.encode(token, "UTF-8") + "&mes=" + clave)
            con = url.openConnection() as HttpURLConnection
            con.connectTimeout = 7000
            con.readTimeout = 9000
            con.instanceFollowRedirects = true
            if (con.responseCode != 200) return false
            val texto = con.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val o = JSONObject(texto)
            if (o.has("error")) return false
            Prefs.guardarCache(ctx, clave, texto)
            return true
        } catch (e: Exception) {
            return false
        } finally {
            con?.disconnect()
        }
    }
}
