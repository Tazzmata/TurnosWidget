package com.ruben.turnos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import java.util.Calendar
import java.util.Locale

/** Dibuja el calendario mensual en un Bitmap (mismo estilo que la captura de referencia). */
object Pintor {
    private fun col(s: String): Int = Color.parseColor(s)

    private fun colorTurno(t: String): Int = when (t) {
        "M" -> col("#FFF59D")
        "T" -> col("#F8A5C8")
        "N" -> col("#80CBC4")
        else -> col("#E0E0E0")
    }

    private fun textoTurno(t: String): Int = when (t) {
        "M" -> col("#6B5A00")
        "T" -> col("#8A1E55")
        "N" -> col("#00504A")
        else -> col("#333333")
    }

    private val PET_CONCEDIDA = col("#A5D6A7")
    private val PET_PEDIDA = col("#FFCC80")
    private val FESTIVO = col("#FFEBEE")
    private val NOTA = col("#26C6DA")
    private val ROJO = col("#D32F2F")
    private val OSCURO = col("#222222")

    fun dibujar(datos: Datos?, vista: String, anio: Int, mes0: Int, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        fun texto(s: String, x: Float, centroY: Float, tam: Float, maxW: Float, alin: Paint.Align, negrita: Boolean, color: Int) {
            p.style = Paint.Style.FILL
            p.typeface = if (negrita) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            p.color = color
            p.textAlign = alin
            var t = tam
            p.textSize = t
            while (p.measureText(s) > maxW && t > 6f) {
                t -= 1f
                p.textSize = t
            }
            c.drawText(s, x, centroY - (p.ascent() + p.descent()) / 2f, p)
        }

        fun rect(r: RectF, color: Int) {
            p.style = Paint.Style.FILL
            p.color = color
            c.drawRect(r, p)
        }

        val borde = w * 0.009f
        val radio = w * 0.02f
        val gap = maxOf(1f, w * 0.0016f)
        val marco = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val ruta = Path()
        ruta.addRoundRect(marco, radio, radio, Path.Direction.CW)
        c.save()
        c.clipPath(ruta)
        rect(marco, col("#8AA9D4"))

        val cabH = h * 0.06f
        val izq = borde
        val arr = borde
        val cw = (w - 2 * borde) / 7f
        val ch = (h - 2 * borde - cabH) / 6f

        val nombresDia = arrayOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
        for (i in 0 until 7) {
            val r = RectF(izq + i * cw + gap / 2, arr + gap / 2, izq + (i + 1) * cw - gap / 2, arr + cabH - gap / 2)
            rect(r, col("#DDDDDD"))
            texto(nombresDia[i], r.centerX(), r.centerY(), cabH * 0.55f, cw * 0.9f, Paint.Align.CENTER, false, OSCURO)
        }

        val cal = Calendar.getInstance()
        cal.set(anio, mes0, 1, 12, 0, 0)
        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -dow)
        val hoy = Calendar.getInstance()
        val claveHoy = String.format(
            Locale.ROOT, "%04d-%02d-%02d",
            hoy.get(Calendar.YEAR), hoy.get(Calendar.MONTH) + 1, hoy.get(Calendar.DAY_OF_MONTH)
        )
        val yo: String? = datos?.yo

        for (k in 0 until 42) {
            val fila = k / 7
            val columna = k % 7
            val x0 = izq + columna * cw
            val y0 = arr + cabH + fila * ch
            val r = RectF(x0 + gap / 2, y0 + gap / 2, x0 + cw - gap / 2, y0 + ch - gap / 2)
            val a = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val d = cal.get(Calendar.DAY_OF_MONTH)
            val clave = String.format(Locale.ROOT, "%04d-%02d-%02d", a, m + 1, d)
            val fuera = m != mes0

            val dia: Dia? = datos?.dias?.get(clave)
            val festivo: String? = dia?.festivo
            val ey: Entrada? = if (yo != null) dia?.usuarios?.get(yo) else null
            val t: String? = ey?.t
            val pet: String? = ey?.p
            val conc: Boolean = ey?.conc == true
            val nota: String? = ey?.n
            val lado = minOf(r.width(), r.height())

            if (fuera) c.saveLayerAlpha(r, 140)

            // Fondo de la celda
            val fondo: Int = if (vista == "solo") {
                when {
                    t != null -> colorTurno(t)
                    pet != null -> if (conc) PET_CONCEDIDA else PET_PEDIDA
                    festivo != null -> FESTIVO
                    else -> Color.WHITE
                }
            } else {
                if (festivo != null) FESTIVO else Color.WHITE
            }
            rect(r, fondo)

            // Número del día
            val colorNum = if (columna == 6 || festivo != null) ROJO else (if (t != null && vista == "solo") textoTurno(t) else OSCURO)
            val tamNum = lado * 0.24f
            texto(d.toString(), r.right - lado * 0.05f, r.top + lado * 0.05f + tamNum * 0.5f, tamNum, r.width() * 0.6f, Paint.Align.RIGHT, false, colorNum)

            // Franja inferior: nota (azul) o, si hay turno y petición, la petición
            val altoFranja = r.height() * 0.24f
            var textoFranja: String? = null
            var colorFranja = NOTA
            if (nota != null) {
                textoFranja = nota
            } else if (vista == "solo" && t != null && pet != null) {
                textoFranja = pet + (if (conc) "" else " ?")
                colorFranja = if (conc) PET_CONCEDIDA else PET_PEDIDA
            }

            if (vista == "solo") {
                val centro = t ?: pet
                if (centro != null) {
                    val cy = r.top + r.height() * (if (textoFranja != null) 0.52f else 0.62f)
                    if (t != null) {
                        texto(t, r.centerX(), cy, lado * 0.30f, r.width() * 0.9f, Paint.Align.CENTER, true, textoTurno(t))
                    } else {
                        texto(centro + (if (conc) "" else " ?"), r.centerX(), cy, lado * 0.22f, r.width() * 0.9f, Paint.Align.CENTER, true, col("#333333"))
                    }
                }
            } else if (datos != null && dia != null) {
                val lista = ArrayList<Pair<Usuario, Entrada>>()
                for (u in datos.usuarios) {
                    val e = dia.usuarios[u.nombre]
                    if (e != null && (e.t != null || e.p != null)) lista.add(Pair(u, e))
                }
                if (lista.isNotEmpty()) {
                    val top = r.top + r.height() * 0.27f
                    val bottom = r.bottom - (if (textoFranja != null) altoFranja else 0f) - 1f
                    val chipH = minOf((bottom - top) / lista.size, r.height() * 0.21f)
                    val pad = r.width() * 0.04f
                    for (i in lista.indices) {
                        val u = lista[i].first
                        val e = lista[i].second
                        val et = e.t
                        val cr = RectF(r.left + pad, top + i * chipH + 0.5f, r.right - pad, top + (i + 1) * chipH - 0.5f)
                        val colorChip = if (et != null) colorTurno(et) else (if (e.conc) PET_CONCEDIDA else PET_PEDIDA)
                        p.style = Paint.Style.FILL
                        p.color = colorChip
                        c.drawRoundRect(cr, chipH * 0.25f, chipH * 0.25f, p)
                        if (u.yo) {
                            p.style = Paint.Style.STROKE
                            p.strokeWidth = maxOf(1.5f, w * 0.002f)
                            p.color = col("#444444")
                            c.drawRoundRect(cr, chipH * 0.25f, chipH * 0.25f, p)
                        }
                        val tam = chipH * 0.66f
                        texto(u.ini, cr.left + cr.width() * 0.06f, cr.centerY(), tam, cr.width() * 0.4f, Paint.Align.LEFT, true, col("#333333"))
                        val valor = (et ?: e.p ?: "") + (if (et == null && !e.conc) "?" else "")
                        texto(valor, cr.left + cr.width() * 0.46f, cr.centerY(), tam, cr.width() * 0.52f, Paint.Align.LEFT, false, col("#333333"))
                    }
                }
            }

            if (textoFranja != null) {
                val fr = RectF(r.left, r.bottom - altoFranja, r.right, r.bottom)
                rect(fr, colorFranja)
                texto(textoFranja, fr.centerX(), fr.centerY(), altoFranja * 0.62f, fr.width() * 0.92f, Paint.Align.CENTER, false, OSCURO)
            }

            if (fuera) c.restore()

            if (clave == claveHoy) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = maxOf(2.5f, w * 0.004f)
                p.color = col("#1565C0")
                val ins = p.strokeWidth / 2f
                c.drawRect(r.left + ins, r.top + ins, r.right - ins, r.bottom - ins, p)
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (datos == null) {
            val caja = RectF(w * 0.08f, h * 0.42f, w * 0.92f, h * 0.58f)
            p.style = Paint.Style.FILL
            p.color = Color.argb(230, 255, 255, 255)
            c.drawRoundRect(caja, radio, radio, p)
            texto("Sin datos: revisa el enlace o la conexión", caja.centerX(), caja.centerY(), w * 0.04f, caja.width() * 0.92f, Paint.Align.CENTER, true, OSCURO)
        }

        // Marco exterior
        p.style = Paint.Style.STROKE
        p.strokeWidth = borde * 2f
        p.color = OSCURO
        c.drawPath(ruta, p)
        c.restore()
        return bmp
    }
}
