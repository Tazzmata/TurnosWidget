package com.ruben.turnos

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Calendar

class AvisoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val accion = intent.action
        if (accion == Intent.ACTION_BOOT_COMPLETED || accion == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Aviso.programar(context)
        } else if (accion == Aviso.ACC_AVISO) {
            Aviso.programar(context)
            if (!Prefs.avisoOn(context)) return
            val pr = goAsync()
            Thread {
                try {
                    Aviso.avisar(context, false)
                } finally {
                    pr?.finish()
                }
            }.start()
        }
    }
}

object Aviso {
    const val ACC_AVISO = "com.ruben.turnos.AVISO"
    private const val CANAL = "avisos_turnos"

    private fun pendiente(ctx: Context): PendingIntent {
        val i = Intent(ctx, AvisoReceiver::class.java)
        i.action = ACC_AVISO
        return PendingIntent.getBroadcast(ctx, 500, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Programa la próxima alarma diaria (o la cancela si los avisos están desactivados). */
    fun programar(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendiente(ctx)
        am.cancel(pi)
        if (!Prefs.avisoOn(ctx)) return
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, Prefs.avisoHora(ctx))
        cal.set(Calendar.MINUTE, Prefs.avisoMin(ctx))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis() + 1000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }

    /** Muestra la notificación con lo que hay mañana. Con manual=true avisa aunque no haya nada. */
    fun avisar(ctx: Context, manual: Boolean) {
        val manana = Calendar.getInstance()
        manana.add(Calendar.DAY_OF_YEAR, 1)
        val a = manana.get(Calendar.YEAR)
        val m = manana.get(Calendar.MONTH)
        val clave = Fechas.claveDe(manana)

        if (Prefs.configurado(ctx)) {
            Red.descargar(ctx, a, m)
        }
        val datos = Prefs.datos(ctx, a, m) ?: return
        val dia = datos.dias[clave]
        val ey: Entrada? = dia?.usuarios?.get(datos.yo)
        val t: String? = ey?.t
        val pet: String? = ey?.p
        val nota: String? = ey?.n
        val conc: Boolean = ey?.conc == true
        if (t == null && pet == null && nota == null && !manual) return

        val lineas = ArrayList<String>()
        if (pet != null) {
            lineas.add(Textos.peticion(pet) + (if (conc) "" else " (pedido, sin conceder)"))
        } else if (t != null) {
            lineas.add("Turno de " + Textos.turno(t) + " (" + t + ")")
        } else if (nota == null) {
            lineas.add("Libre")
        }
        if (nota != null) {
            lineas.add("Nota: " + nota)
        }
        if (t != null && pet == null && dia != null) {
            val companeros = ArrayList<String>()
            for (u in datos.usuarios) {
                if (u.yo) continue
                val otro = dia.usuarios[u.nombre]
                if (otro != null && otro.t == t && otro.p == null) companeros.add(u.nombre)
            }
            if (companeros.isNotEmpty()) {
                lineas.add("Contigo: " + companeros.joinToString(", "))
            }
        }

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CANAL, "Avisos de turnos", NotificationManager.IMPORTANCE_DEFAULT))

        val abrir = Intent(ctx, DiaActivity::class.java)
        abrir.data = Uri.parse("turnos://dia/$clave")
        abrir.putExtra("fecha", clave)
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(ctx, 900, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val titulo = "Mañana, " + Textos.fechaLarga(clave).substringBefore(" de ")
        val n = Notification.Builder(ctx, CANAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(titulo)
            .setContentText(lineas.joinToString(" · "))
            .setStyle(Notification.BigTextStyle().bigText(lineas.joinToString("\n")))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(1001, n)
    }
}
