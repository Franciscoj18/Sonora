# Sonora 🎵

Reproductor de música para Android inspirado en el estilo del reproductor del iPhone, pensado para correr fluido en equipos de gama media como el **Moto G9**.

## Funciones

- Lee automáticamente las canciones MP3 del teléfono y detecta descargas nuevas.
- Biblioteca con **Playlists, Artistas, Álbumes y Canciones**, más "Agregado recientemente".
- Reproductor a pantalla completa: carátula grande que se encoge al pausar, fondo con los colores de la carátula, barra de progreso delgada, aleatorio, repetir y cola "A continuación". Desliza hacia abajo para cerrarlo.
- Mini reproductor flotante, controles en la notificación y en la pantalla de bloqueo, pausa al desconectar audífonos.
- **Álbumes propios**: mantén presionada una canción, selecciona varias y toca *Crear álbum*. Ponle nombre, artista, año y carátula desde tu galería.
- **Cambiar carátula** de cualquier álbum o playlist (menú ⋯). La carátula queda fija.
- **Editar canción**: título, artista y número de pista, con botón para limpiar títulos de YouTube como "(Official Video)" o "Artista - Canción".
- Búsqueda por canción, artista o álbum (sin importar acentos).
- Tema claro y oscuro según el sistema.

Los cambios (álbumes, carátulas, ediciones) se guardan dentro de la app; tus archivos originales no se modifican.

## Descargar el APK

Cada vez que se sube código a la rama `main`, GitHub compila la app automáticamente:

1. Entra a la pestaña **Releases** del repositorio (o **Actions** → última compilación).
2. Descarga `Sonora.apk` desde el teléfono.
3. Ábrelo y permite "Instalar apps desconocidas" cuando Android lo pida.

## Tecnología

Kotlin · Jetpack Compose · Media3 (ExoPlayer) · Room · Coil. Versión mínima: Android 10.

> Nota: la clave de firma (`keystore/`) está en el repositorio para que cada compilación se pueda instalar encima de la anterior. Si haces el repositorio público, considera mantenerlo privado o mover la clave a un *secret* de GitHub.
