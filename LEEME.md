# Turnos · Widget de calendario (Android)

## 1. Apps Script (una sola vez)
1. En Codigo.gs, sustituye `doGet` por la versión nueva y pega al final las funciones de `api.gs`.
2. Implementar > Gestionar implementaciones > editar > Nueva versión > Implementar
   (acceso: "Cualquier persona", igual que para el calendario ICS).

## 2. Compilar el APK (sin instalar nada en tu PC)
1. Crea un repositorio en GitHub (puede ser privado) y sube TODO el contenido de esta carpeta
   (incluida la carpeta oculta `.github`).
2. Pestaña "Actions" > "Compilar APK" > Run workflow (o se lanza solo al subir).
3. Al terminar (unos 3-5 min), entra en la ejecución y descarga el artefacto `turnos-widget-apk`.
   Dentro está `app-debug.apk`.
Alternativa: abre la carpeta con Android Studio y pulsa Build > Build APK.

## 3. Instalar y usar
1. Pasa el APK al móvil e instálalo (permite "instalar apps desconocidas" si lo pide).
2. Abre "Turnos", pega el enlace de "Mi calendario" de la app web y pulsa "Guardar y probar".
3. Pantalla de inicio > mantén pulsado > Widgets > Turnos. Mejor en tamaño 4x4.

## Controles del widget
- ‹ › cambian de mes · pulsar el título vuelve a hoy y refresca · 👤/👥 alterna "solo yo" / "todos"
- Pulsar el calendario abre la app web de turnos.
- Se actualiza solo cada ~30 min y al navegar.
