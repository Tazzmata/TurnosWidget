package com.ruben.turnos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Calendar

/** Detalle de un día: turnos de todos, peticiones y notas completas. */
class DiaActivity : Activity() {

    private lateinit var titulo: TextView
    private lateinit var cuerpo: LinearLayout
    private var clave: String = ""

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clave = intent.getStringExtra("fecha") ?: Fechas.hoy()
        setContentView(construirUi())
        mostrar(true)
    }

    private fun construirUi(): View {
        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setPadding(dp(16), dp(24), dp(16), dp(16))

        val nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.gravity = Gravity.CENTER_VERTICAL
        val ant = Button(this)
        ant.text = "‹"
        ant.setOnClickListener { mover(-1) }
        titulo = TextView(this)
        titulo.gravity = Gravity.CENTER
        titulo.textSize = 18f
        titulo.typeface = Typeface.DEFAULT_BOLD
        titulo.setTextColor(Color.parseColor("#111111"))
        val sig = Button(this)
        sig.text = "›"
        sig.setOnClickListener { mover(1) }
        nav.addView(ant, LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT))
        nav.addView(titulo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(sig, LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT))
        raiz.addView(nav)

        cuerpo = LinearLayout(this)
        cuerpo.orientation = LinearLayout.VERTICAL
        cuerpo.setPadding(0, dp(12), 0, dp(12))
        val scroll = ScrollView(this)
        scroll.addView(cuerpo)
        raiz.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        val abrir = Button(this)
        abrir.text = "Abrir la app de turnos"
        abrir.setOnClickListener {
            if (Prefs.base(this).isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.base(this))))
            }
        }
        raiz.addView(abrir)
        return raiz
    }

    private fun mover(delta: Int) {
        val cal = Fechas.calendario(clave)
        cal.add(Calendar.DAY_OF_MONTH, delta)
        clave = Fechas.claveDe(cal)
        mostrar(true)
    }

    /** Busca los datos en la caché del mes del día o de los meses vecinos. */
    private fun datosDe(anio: Int, mes0: Int): Datos? {
        val desplazamientos = intArrayOf(0, -1, 1)
        for (o in desplazamientos) {
            val c = Calendar.getInstance()
            c.set(anio, mes0, 1, 12, 0, 0)
            c.add(Calendar.MONTH, o)
            val d = Prefs.datos(this, c.get(Calendar.YEAR), c.get(Calendar.MONTH))
            if (d != null) return d
        }
        return null
    }

    private fun linea(texto: String, negrita: Boolean, color: String, tam: Float) {
        val tv = TextView(this)
        tv.text = texto
        tv.textSize = tam
        tv.setTextColor(Color.parseColor(color))
        if (negrita) tv.typeface = Typeface.DEFAULT_BOLD
        tv.setPadding(0, dp(2), 0, dp(2))
        cuerpo.addView(tv)
    }

    private fun mostrar(descargar: Boolean) {
        val cal = Fechas.calendario(clave)
        val anio = cal.get(Calendar.YEAR)
        val mes0 = cal.get(Calendar.MONTH)
        titulo.text = Textos.fechaLarga(clave)
        cuerpo.removeAllViews()

        val datos = datosDe(anio, mes0)
        if (datos == null) {
            linea("Sin datos descargados todavía.", false, "#555555", 15f)
            if (descargar && Prefs.configurado(this)) {
                Thread {
                    Red.descargar(this, anio, mes0)
                    runOnUiThread { mostrar(false) }
                }.start()
            }
            return
        }

        val dia = datos.dias[clave]
        if (dia?.festivo != null) {
            linea("Festivo: " + dia.festivo, true, "#D32F2F", 15f)
        }
        for (u in datos.usuarios) {
            val e = dia?.usuarios?.get(u.nombre)
            linea(u.nombre + (if (u.yo) " (tú)" else ""), true, "#111111", 17f)
            var hayAlgo = false
            val et = e?.t
            val ep = e?.p
            val en = e?.n
            val concE = e?.conc == true
            if (ep != null) {
                linea("• " + Textos.peticion(ep) + (if (concE) " (concedido)" else " (pedido, sin conceder)"), false, "#2E7D32", 15f)
                hayAlgo = true
            }
            if (et != null) {
                val aviso = if (ep != null) " (sustituido por la petición)" else ""
                linea("• Turno de " + Textos.turno(et) + " (" + et + ")" + aviso, false, "#333333", 15f)
                hayAlgo = true
            }
            if (en != null) {
                linea("• Nota: " + en, false, "#006064", 15f)
                hayAlgo = true
            }
            if (!hayAlgo) {
                linea("• Libre", false, "#78909C", 15f)
            }
        }

        if (datos.saldo.isNotEmpty()) {
            linea("Tu saldo de días ($anio)", true, "#111111", 17f)
            for (s in datos.saldo) {
                linea("• " + Textos.peticion(s.tipo) + ": quedan " + s.quedan + " de " + s.cupo, false, "#333333", 15f)
            }
        }
    }
}
