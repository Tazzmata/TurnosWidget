package com.ruben.turnos

import org.json.JSONObject

class Usuario(val nombre: String, val ini: String, val yo: Boolean)

class Entrada(val t: String?, val p: String?, val conc: Boolean, val n: String?)

class Dia(val festivo: String?, val usuarios: Map<String, Entrada>)

class Datos(val yo: String, val usuarios: List<Usuario>, val dias: Map<String, Dia>)

object Modelo {
    fun parsear(json: String): Datos? {
        return try {
            val o = JSONObject(json)
            val lu = o.getJSONArray("usuarios")
            val usuarios = ArrayList<Usuario>()
            for (i in 0 until lu.length()) {
                val u = lu.getJSONObject(i)
                usuarios.add(Usuario(u.getString("n"), u.getString("i"), u.optInt("yo", 0) == 1))
            }
            val dias = HashMap<String, Dia>()
            val jd = o.getJSONObject("dias")
            val itDias = jd.keys()
            while (itDias.hasNext()) {
                val clave = itDias.next()
                val jdia = jd.getJSONObject(clave)
                val festivo: String? = if (jdia.has("f")) jdia.getString("f") else null
                val mapa = HashMap<String, Entrada>()
                val ju = jdia.getJSONObject("u")
                val itUsu = ju.keys()
                while (itUsu.hasNext()) {
                    val nombre = itUsu.next()
                    val e = ju.getJSONObject(nombre)
                    val t: String? = if (e.has("t")) e.getString("t") else null
                    val p: String? = if (e.has("p")) e.getString("p") else null
                    val n: String? = if (e.has("n")) e.getString("n") else null
                    mapa[nombre] = Entrada(t, p, e.optInt("c", 0) == 1, n)
                }
                dias[clave] = Dia(festivo, mapa)
            }
            Datos(o.getString("yo"), usuarios, dias)
        } catch (e: Exception) {
            null
        }
    }
}
