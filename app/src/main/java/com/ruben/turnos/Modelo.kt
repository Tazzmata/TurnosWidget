package com.ruben.turnos

import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class Usuario(val nombre: String, val ini: String, val yo: Boolean)

class Entrada(val t: String?, val p: String?, val conc: Boolean, val n: String?)

class Dia(val festivo: String?, val usuarios: Map<String, Entrada>)

class Saldo(val tipo: String, val quedan: Int, val cupo: Int)

class Datos(val yo: String, val usuarios: List<Usuario>, val dias: Map<String, Dia>, val saldo: List<Saldo>)

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
            val saldo = ArrayList<Saldo>()
            val js = o.optJSONArray("saldo")
            if (js != null) {
                for (i in 0 until js.length()) {
                    val s = js.getJSONObject(i)
                    saldo.add(Saldo(s.getString("t"), s.getInt("q"), s.getInt("c")))
                }
            }
            Datos(o.getString("yo"), usuarios, dias, saldo)
        } catch (e: Exception) {
            null
        }
    }
}

object Textos {
    val DIAS = arrayOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")
    val MESES = arrayOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )

    fun turno(t: String): String = when (t) {
        "M" -> "mañana"
        "T" -> "tarde"
        "N" -> "noche"
        else -> t
    }

    fun peticion(abrev: String): String = when (abrev) {
        "Vac" -> "Vacaciones"
        "AP" -> "Asuntos propios"
        "Comp" -> "Compensación"
        "Car" -> "Día de caridad"
        else -> "Día libre"
    }

    fun fechaLarga(clave: String): String {
        val cal = Fechas.calendario(clave)
        return DIAS[cal.get(Calendar.DAY_OF_WEEK) - 1] + " " + cal.get(Calendar.DAY_OF_MONTH) +
            " de " + MESES[cal.get(Calendar.MONTH)] + " de " + cal.get(Calendar.YEAR)
    }
}

object Fechas {
    fun clave(anio: Int, mes0: Int, dia: Int): String =
        String.format(Locale.ROOT, "%04d-%02d-%02d", anio, mes0 + 1, dia)

    fun hoy(): String {
        val c = Calendar.getInstance()
        return clave(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }

    fun calendario(clave: String): Calendar {
        val c = Calendar.getInstance()
        val partes = clave.split("-")
        c.set(partes[0].toInt(), partes[1].toInt() - 1, partes[2].toInt(), 12, 0, 0)
        return c
    }

    fun claveDe(c: Calendar): String = clave(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))

    /** Fecha de la celda k (0-41) de la rejilla del mes, que empieza en lunes. */
    fun claveCelda(anio: Int, mes0: Int, k: Int): String {
        val cal = Calendar.getInstance()
        cal.set(anio, mes0, 1, 12, 0, 0)
        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, k - dow)
        return claveDe(cal)
    }
}
