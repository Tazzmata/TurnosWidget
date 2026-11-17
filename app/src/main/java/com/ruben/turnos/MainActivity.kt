package com.ruben.turnos

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var campo: EditText
    private lateinit var estado: TextView
    private lateinit var titulo: TextView
    private lateinit var vistaPrevia: ImageView
    private lateinit var rbSolo: RadioButton
    private lateinit var rbTodos: RadioButton
    private lateinit var botonHora: Button
    private lateinit var etiquetaOpacidad: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(construirUi())
        if (Prefs.configurado(this)) {
            campo.setText(Prefs.base(this) + "?cal=" + Prefs.token(this))
        }
        rbSolo.isChecked = Prefs.vista(this) == "solo"
        rbTodos.isChecked = Prefs.vista(this) != "solo"
        refrescarVistaPrevia()
    }

    private fun seccion(raiz: LinearLayout, texto: String) {
        val tv = TextView(this)
        tv.text = texto
        tv.textSize = 17f
        tv.setTextColor(Color.parseColor("#111111"))
        tv.setPadding(0, dp(18), 0, dp(4))
        raiz.addView(tv)
    }

    private fun interruptor(texto: String, marcado: Boolean, alCambiar: (Boolean) -> Unit): Switch {
        val s = Switch(this)
        s.text = texto
        s.isChecked = marcado
        s.setPadding(0, dp(6), 0, dp(6))
        s.setOnCheckedChangeListener { _, v -> alCambiar(v) }
        return s
    }

    private fun textoHora(): String =
        String.format("%02d:%02d", Prefs.avisoHora(this), Prefs.avisoMin(this))

    private fun aplicarCambios() {
        refrescarVistaPrevia()
    }

    private fun construirUi(): View {
        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setPadding(dp(16), dp(24), dp(16), dp(24))

        val h1 = TextView(this)
        h1.text = "Turnos · Widget de calendario"
        h1.textSize = 22f
        h1.setTextColor(Color.parseColor("#111111"))
        raiz.addView(h1)

        val ayuda = TextView(this)
        ayuda.text = "1) En la app de turnos, abre «Mi calendario», copia el enlace y pégalo aquí.\n" +
            "2) Pulsa Guardar y probar.\n" +
            "3) Mantén pulsada la pantalla de inicio → Widgets → Turnos y colócalo.\n" +
            "Pulsa cualquier día del widget para ver su detalle."
        ayuda.textSize = 14f
        ayuda.setPadding(0, dp(8), 0, dp(12))
        raiz.addView(ayuda)

        campo = EditText(this)
        campo.hint = "https://script.google.com/.../exec?cal=..."
        campo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        campo.setSingleLine(false)
        campo.minLines = 2
        raiz.addView(campo)

        val guardar = Button(this)
        guardar.text = "Guardar y probar"
        guardar.setOnClickListener { guardarYProbar() }
        raiz.addView(guardar)

        estado = TextView(this)
        estado.textSize = 14f
        estado.setPadding(0, dp(6), 0, dp(6))
        raiz.addView(estado)

        // ----- Avisos -----
        seccion(raiz, "Aviso la víspera")
        raiz.addView(interruptor("Avisarme el día antes (turno, petición o nota)", Prefs.avisoOn(this)) { activo ->
            Prefs.setAvisoOn(this, activo)
            if (activo && Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
            }
            Aviso.programar(this)
        })
        botonHora = Button(this)
        botonHora.text = "Hora del aviso: " + textoHora()
        botonHora.setOnClickListener {
            TimePickerDialog(
                this,
                TimePickerDialog.OnTimeSetListener { _, h, m ->
                    Prefs.setAvisoHora(this, h, m)
                    botonHora.text = "Hora del aviso: " + textoHora()
                    Aviso.programar(this)
                },
                Prefs.avisoHora(this),
                Prefs.avisoMin(this),
                true
            ).show()
        }
        raiz.addView(botonHora)
        val probar = Button(this)
        probar.text = "Probar aviso ahora"
        probar.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
            }
            Thread { Aviso.avisar(this, true) }.start()
        }
        raiz.addView(probar)

        // ----- Aspecto -----
        seccion(raiz, "Aspecto del widget")
        raiz.addView(interruptor("Tema oscuro", Prefs.oscuro(this)) { v ->
            Prefs.setOscuro(this, v)
            aplicarCambios()
        })
        raiz.addView(interruptor("Mostrar saldo de días", Prefs.mostrarSaldo(this)) { v ->
            Prefs.setMostrarSaldo(this, v)
            aplicarCambios()
        })
        etiquetaOpacidad = TextView(this)
        etiquetaOpacidad.textSize = 14f
        etiquetaOpacidad.text = "Opacidad del fondo: " + Prefs.fondoAlfa(this) + "%"
        etiquetaOpacidad.setPadding(0, dp(8), 0, 0)
        raiz.addView(etiquetaOpacidad)
        val barra = SeekBar(this)
        barra.max = 100
        barra.progress = Prefs.fondoAlfa(this)
        barra.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                etiquetaOpacidad.text = "Opacidad del fondo: " + progress + "%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                Prefs.setFondoAlfa(this@MainActivity, barra.progress)
                aplicarCambios()
            }
        })
        raiz.addView(barra)

        // ----- Vista previa -----
        seccion(raiz, "Vista previa")
        val grupo = RadioGroup(this)
        grupo.orientation = RadioGroup.HORIZONTAL
        rbSolo = RadioButton(this)
        rbSolo.text = "Solo yo"
        rbSolo.id = View.generateViewId()
        rbTodos = RadioButton(this)
        rbTodos.text = "Todos los compañeros"
        rbTodos.id = View.generateViewId()
        grupo.addView(rbSolo)
        grupo.addView(rbTodos)
        grupo.setOnCheckedChangeListener { _, checkedId ->
            Prefs.setVista(this, if (checkedId == rbSolo.id) "solo" else "todos")
            refrescarVistaPrevia()
        }
        raiz.addView(grupo)

        val nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.gravity = Gravity.CENTER_VERTICAL
        val ant = Button(this)
        ant.text = "‹"
        ant.setOnClickListener { cambiarMes(-1) }
        titulo = TextView(this)
        titulo.gravity = Gravity.CENTER
        titulo.textSize = 18f
        titulo.setTextColor(Color.parseColor("#111111"))
        val sig = Button(this)
        sig.text = "›"
        sig.setOnClickListener { cambiarMes(1) }
        nav.addView(ant, LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT))
        nav.addView(titulo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        nav.addView(sig, LinearLayout.LayoutParams(dp(56), LinearLayout.LayoutParams.WRAP_CONTENT))
        raiz.addView(nav)

        vistaPrevia = ImageView(this)
        vistaPrevia.adjustViewBounds = true
        vistaPrevia.setBackgroundColor(Color.parseColor("#6FA0DC"))
        raiz.addView(vistaPrevia, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val abrir = Button(this)
        abrir.text = "Abrir la app de turnos"
        abrir.setOnClickListener {
            if (Prefs.base(this).isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.base(this))))
            }
        }
        raiz.addView(abrir)

        val scroll = ScrollView(this)
        scroll.addView(raiz)
        return scroll
    }

    private fun guardarYProbar() {
        val texto = campo.text.toString().trim()
        val uri: Uri? = try { Uri.parse(texto) } catch (e: Exception) { null }
        val token = uri?.getQueryParameter("cal") ?: uri?.getQueryParameter("api")
        if (uri == null || token.isNullOrBlank() || !texto.startsWith("http")) {
            estado.setTextColor(Color.parseColor("#B71C1C"))
            estado.text = "No parece el enlace de «Mi calendario» (debe terminar en ?cal=...)."
            return
        }
        Prefs.guardarEnlace(this, texto.substringBefore('?'), token)
        Prefs.setOffset(this, 0)
        estado.setTextColor(Color.parseColor("#333333"))
        estado.text = "Descargando…"
        val par = Prefs.mes(this)
        Thread {
            val ok = Red.descargar(this, par.first, par.second)
            Prefs.setFallo(this, !ok)
            runOnUiThread {
                if (ok) {
                    estado.setTextColor(Color.parseColor("#1B5E20"))
                    estado.text = "Conectado correctamente."
                } else {
                    estado.setTextColor(Color.parseColor("#B71C1C"))
                    estado.text = "No se pudo conectar. Comprueba que la app web está publicada para «Cualquier persona» y que has desplegado la última versión."
                }
                refrescarVistaPrevia()
            }
        }.start()
    }

    private fun cambiarMes(delta: Int) {
        Prefs.setOffset(this, Prefs.offset(this) + delta)
        refrescarVistaPrevia()
        val par = Prefs.mes(this)
        if (Prefs.configurado(this) && Prefs.cache(this, Prefs.claveMes(par.first, par.second)) == null) {
            Thread {
                val ok = Red.descargar(this, par.first, par.second)
                if (ok) runOnUiThread { refrescarVistaPrevia() }
            }.start()
        }
    }

    private fun refrescarVistaPrevia() {
        val par = Prefs.mes(this)
        titulo.text = CalendarWidget.nombreMes(par.first, par.second)
        val datos = Prefs.datos(this, par.first, par.second)
        vistaPrevia.setImageBitmap(
            Pintor.dibujar(datos, Prefs.vista(this), par.first, par.second, 900, 900, Prefs.fondoAlfa(this), Prefs.oscuro(this))
        )
        CalendarWidget.pintarTodos(this)
    }
}
