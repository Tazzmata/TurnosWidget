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
    private val NOTA = col("#26C6DA")
    private val ROJO = col("#D32F2F")
    private val OSCURO = col("#222222")

    private fun conAlfa(color: Int, a: Int): Int =
        Color.argb(a * Color.alpha(color) / 255, Color.red(color), Color.green(color), Color.blue(color))

    fun dibujar(datos: Datos?, vista: String, anio: Int, mes0: Int, w: Int, h: Int, alfaPct: Int, oscuroTema: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val alfa = alfaPct.coerceIn(0, 100) * 255 / 100

        // Paleta según el tema
        val cBase = if (oscuroTema) col("#26282B") else Color.WHITE
        val cCab = if (oscuroTema) col("#3C4043") else col("#DDDDDD")
        val cTxtCab = if (oscuroTema) col("#E8EAED") else OSCURO
        val cLinea = if (oscuroTema) col("#5F6368") else col("#8AA9D4")
        val cNum = if (oscuroTema) col("#E8EAED") else OSCURO
        val cFestivo = if (oscuroTema) col("#4A2C30") else col("#FFEBEE")
        val cDom = if (oscuroTema) col("#EF5350") else ROJO
        val cLibreFondo = if (oscuroTema) col("#3A3D40") else col("#ECEFF1")
        val cLibreTxt = if (oscuroTema) col("#9AA0A6") else col("#78909C")
        val cMarco = if (oscuroTema) col("#9AA0A6") else OSCURO
        val cHoy = if (oscuroTema) col("#64B5F6") else col("#1565C0")

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
        val marco = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val ruta = Path()
        ruta.addRoundRect(marco, radio, radio, Path.Direction.CW)
        c.save()
        c.clipPath(ruta)

        val cabH = h * 0.06f
        val izq = borde
        val arr = borde
        val cw = (w - 2 * borde) / 7f
        val ch = (h - 2 * borde - cabH) / 6f

        val nombresDia = arrayOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
        for (i in 0 until 7) {
            val r = RectF(izq + i * cw, arr, izq + (i + 1) * cw, arr + cabH)
            rect(r, conAlfa(cCab, maxOf(alfa, 160)))
            texto(nombresDia[i], r.centerX(), r.centerY(), cabH * 0.55f, cw * 0.9f, Paint.Align.CENTER, false, cTxtCab)
        }

        val cal = Calendar.getInstance()
        cal.set(anio, mes0, 1, 12, 0, 0)
        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        cal.add(Calendar.DAY_OF_MONTH, -dow)
        val claveHoy = Fechas.hoy()
        val yo: String? = datos?.yo

        for (k in 0 until 42) {
            val fila = k / 7
            val columna = k % 7
            val x0 = izq + columna * cw
            val y0 = arr + cabH + fila * ch
            val r = RectF(x0, y0, x0 + cw, y0 + ch)
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

            // Fondo de la celda: la petición tapa al turno
            val celdaColor = vista == "solo" && (pet != null || t != null)
            val fondo: Int = if (vista == "solo") {
                when {
                    pet != null -> if (conc) PET_CONCEDIDA else PET_PEDIDA
                    t != null -> colorTurno(t)
                    festivo != null -> cFestivo
                    else -> cBase
                }
            } else {
                if (festivo != null) cFestivo else cBase
            }
            rect(r, conAlfa(fondo, alfa))

            // Número del día
            val colorNum: Int = when {
                columna == 6 || festivo != null -> if (celdaColor) ROJO else cDom
                celdaColor -> if (pet == null && t != null) textoTurno(t) else OSCURO
                else -> cNum
            }
            val tamNum = lado * 0.24f
            texto(d.toString(), r.right - lado * 0.05f, r.top + lado * 0.05f + tamNum * 0.5f, tamNum, r.width() * 0.6f, Paint.Align.RIGHT, false, colorNum)

            // Franja inferior con la nota
            val altoFranja = r.height() * 0.24f
            val textoFranja: String? = nota

            if (vista == "solo") {
                val cy = r.top + r.height() * (if (textoFranja != null) 0.52f else 0.62f)
                if (pet != null) {
                    texto(pet + (if (conc) "" else " ?"), r.centerX(), cy, lado * 0.22f, r.width() * 0.9f, Paint.Align.CENTER, true, OSCURO)
                } else if (t != null) {
                    texto(t, r.centerX(), cy, lado * 0.30f, r.width() * 0.9f, Paint.Align.CENTER, true, textoTurno(t))
                }
            } else if (datos != null) {
                val lista = ArrayList<Pair<Usuario, Entrada?>>()
                for (u in datos.usuarios) {
                    lista.add(Pair(u, dia?.usuarios?.get(u.nombre)))
                }
                if (lista.isNotEmpty()) {
                    val top = r.top + r.height() * 0.27f
                    val bottom = r.bottom - (if (textoFranja != null) altoFranja else 0f) - 1f
                    val chipH = minOf((bottom - top) / lista.size, r.height() * 0.21f)
                    val pad = r.width() * 0.04f
                    for (i in lista.indices) {
                        val u = lista[i].first
                        val e = lista[i].second
                        val et: String? = e?.t
                        val ep: String? = e?.p
                        val concE: Boolean = e?.conc == true
                        val libre = et == null && ep == null
                        val cr = RectF(r.left + pad, top + i * chipH + 0.5f, r.right - pad, top + (i + 1) * chipH - 0.5f)
                        val colorChip: Int = when {
                            ep != null -> if (concE) PET_CONCEDIDA else PET_PEDIDA
                            et != null -> colorTurno(et)
                            else -> cLibreFondo
                        }
                        p.style = Paint.Style.FILL
                        p.color = colorChip
                        c.drawRoundRect(cr, chipH * 0.25f, chipH * 0.25f, p)
                        if (u.yo) {
                            p.style = Paint.Style.STROKE
                            p.strokeWidth = maxOf(1.5f, w * 0.002f)
                            p.color = if (oscuroTema) col("#BDC1C6") else col("#444444")
                            c.drawRoundRect(cr, chipH * 0.25f, chipH * 0.25f, p)
                        }
                        val tam = chipH * 0.66f
                        val colorTxt = if (libre) cLibreTxt else col("#333333")
                        texto(u.ini, cr.left + cr.width() * 0.06f, cr.centerY(), tam, cr.width() * 0.4f, Paint.Align.LEFT, true, colorTxt)
                        val valor: String = if (ep != null) ep + (if (concE) "" else "?") else if (et != null) et else "L"
                        texto(valor, cr.left + cr.width() * 0.46f, cr.centerY(), tam, cr.width() * 0.52f, Paint.Align.LEFT, false, colorTxt)
                    }
                }
            }

            if (textoFranja != null) {
                val fr = RectF(r.left, r.bottom - altoFranja, r.right, r.bottom)
                rect(fr, NOTA)
                texto(textoFranja, fr.centerX(), fr.centerY(), altoFranja * 0.62f, fr.width() * 0.92f, Paint.Align.CENTER, false, OSCURO)
            }

            if (fuera) c.restore()

            if (clave == claveHoy) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = maxOf(2.5f, w * 0.004f)
                p.color = cHoy
                val ins = p.strokeWidth / 2f
                c.drawRect(r.left + ins, r.top + ins, r.right - ins, r.bottom - ins, p)
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Líneas de la rejilla (encima de las celdas, para que no se tiñan con la transparencia)
        p.style = Paint.Style.STROKE
        p.strokeWidth = maxOf(1.5f, w * 0.0016f)
        p.color = conAlfa(cLinea, maxOf(alfa, 120))
        for (i in 1 until 7) {
            c.drawLine(izq + i * cw, arr, izq + i * cw, arr + cabH + 6 * ch, p)
        }
        for (j in 0 until 6) {
            val y = arr + cabH + j * ch
            c.drawLine(izq, y, izq + 7 * cw, y, p)
        }

        if (datos == null) {
            val caja = RectF(w * 0.08f, h * 0.42f, w * 0.92f, h * 0.58f)
            p.style = Paint.Style.FILL
            p.color = if (oscuroTema) Color.argb(235, 38, 40, 43) else Color.argb(230, 255, 255, 255)
            c.drawRoundRect(caja, radio, radio, p)
            texto("Sin datos: revisa el enlace o la conexión", caja.centerX(), caja.centerY(), w * 0.04f, caja.width() * 0.92f, Paint.Align.CENTER, true, cTxtCab)
        }

        // Marco exterior
        p.style = Paint.Style.STROKE
        p.strokeWidth = borde * 2f
        p.color = cMarco
        c.drawPath(ruta, p)
        c.restore()
        return bmp
    }
}
