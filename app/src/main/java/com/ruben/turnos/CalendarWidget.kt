package com.ruben.turnos

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews

class CalendarWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        trabajar(context, true)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        pintarTodos(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACC_PREV -> {
                Prefs.setOffset(context, Prefs.offset(context) - 1)
                trabajar(context, false)
            }
            ACC_NEXT -> {
                Prefs.setOffset(context, Prefs.offset(context) + 1)
                trabajar(context, false)
            }
            ACC_HOY -> {
                Prefs.setOffset(context, 0)
                trabajar(context, true)
            }
            ACC_VISTA -> {
                Prefs.setVista(context, if (Prefs.vista(context) == "solo") "todos" else "solo")
                trabajar(context, false)
            }
            else -> super.onReceive(context, intent)
        }
    }

    /** Pinta al instante con lo guardado y, si hace falta, descarga datos frescos y vuelve a pintar. */
    private fun trabajar(context: Context, forzar: Boolean) {
        pintarTodos(context)
        if (!Prefs.configurado(context)) return
        val par = Prefs.mes(context)
        val clave = Prefs.claveMes(par.first, par.second)
        val antiguo = System.currentTimeMillis() - Prefs.cacheTs(context, clave) > 120000L
        if (forzar || antiguo) {
            val pr = goAsync()
            Thread {
                try {
                    val ok = Red.descargar(context, par.first, par.second)
                    Prefs.setFallo(context, !ok)
                    pintarTodos(context)
                } finally {
                    pr?.finish()
                }
            }.start()
        }
    }

    companion object {
        const val ACC_PREV = "com.ruben.turnos.PREV"
        const val ACC_NEXT = "com.ruben.turnos.NEXT"
        const val ACC_HOY = "com.ruben.turnos.HOY"
        const val ACC_VISTA = "com.ruben.turnos.VISTA"

        private val MESES = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )

        fun nombreMes(anio: Int, mes0: Int): String = MESES[mes0] + " " + anio

        fun pintarTodos(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, CalendarWidget::class.java))
            for (id in ids) {
                mgr.updateAppWidget(id, construir(ctx, mgr, id))
            }
        }

        private fun accion(ctx: Context, accion: String, codigo: Int): PendingIntent {
            val i = Intent(ctx, CalendarWidget::class.java)
            i.action = accion
            return PendingIntent.getBroadcast(ctx, codigo, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        private fun construir(ctx: Context, mgr: AppWidgetManager, id: Int): RemoteViews {
            val vistas = RemoteViews(ctx.packageName, R.layout.widget)
            val par = Prefs.mes(ctx)
            val vista = Prefs.vista(ctx)
            val datos = Prefs.datos(ctx, par.first, par.second)

            val opts = mgr.getAppWidgetOptions(id)
            val wDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            val hDp = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 330)
            val dens = ctx.resources.displayMetrics.density
            var w = (wDp * dens)
            var h = ((hDp - 52) * dens)
            val escala = minOf(1f, 1000f / maxOf(w, h))
            w *= escala
            h *= escala
            val wi = maxOf(300, w.toInt())
            val hi = maxOf(250, h.toInt())

            vistas.setImageViewBitmap(R.id.img_calendario, Pintor.dibujar(datos, vista, par.first, par.second, wi, hi))
            val titulo = nombreMes(par.first, par.second) + (if (Prefs.fallo(ctx)) " ⚠" else "")
            vistas.setTextViewText(R.id.txt_titulo, titulo)
            vistas.setTextViewText(R.id.btn_vista, if (vista == "solo") "👤" else "👥")

            vistas.setOnClickPendingIntent(R.id.btn_prev, accion(ctx, ACC_PREV, 1))
            vistas.setOnClickPendingIntent(R.id.btn_next, accion(ctx, ACC_NEXT, 2))
            vistas.setOnClickPendingIntent(R.id.txt_titulo, accion(ctx, ACC_HOY, 3))
            vistas.setOnClickPendingIntent(R.id.btn_vista, accion(ctx, ACC_VISTA, 4))

            val abrir: Intent = if (Prefs.base(ctx).isNotBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.base(ctx)))
            } else {
                Intent(ctx, MainActivity::class.java)
            }
            abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pi = PendingIntent.getActivity(ctx, 10, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            vistas.setOnClickPendingIntent(R.id.img_calendario, pi)
            return vistas
        }
    }
}
