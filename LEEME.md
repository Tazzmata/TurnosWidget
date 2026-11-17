# Turnos · Widget de calendario (Android) — versión 2

## Novedades
- Clave de firma fija (app/debug.keystore): desde esta versión, las actualizaciones se instalan encima.
- Aviso la víspera (notificación con turno, petición o nota del día siguiente) con hora configurable.
- Pulsar un día del widget abre su detalle (turnos de todos, notas completas, peticiones, saldo).
- Tema oscuro y opacidad del fondo ajustables.
- Saldo de días (vacaciones, asuntos propios...) en el pie del widget.

## Actualizar
1. Apps Script: sustituye `servirApi_` por la nueva y despliega Nueva versión.
2. GitHub: sube TODO el contenido de esta carpeta (se sobrescribe lo existente; incluye app/debug.keystore).
3. Actions compila el APK. Esta primera vez hay que DESINSTALAR la app anterior (cambia la firma);
   desde la siguiente se instalará encima sin perder nada.
4. Abre la app, pega el enlace de «Mi calendario», Guardar y probar, y revisa los ajustes.
