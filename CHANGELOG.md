# Changelog

## 0.1.0-beta.7

- Editor administrativo dentro del juego: botón Editar y `/ZianGui edit [menú]`.
- Añadir, renombrar, cambiar comando/permisos/icono, ordenar y eliminar botones.
- Icono tomado del objeto en la mano principal al guardar, sin consumirlo.
- Guardado atómico en el archivo original, con respaldo `.json.bak` y validación completa.
- Rechaza ediciones concurrentes y cambios externos sin sobrescribirlos.
- Comprueba `zian.gui.edit` en cada guardado; sesiones personales y límites de frecuencia.
- Permite menús vacíos para quitar todas las integraciones y añadir otras después.
- Protocolo 3: requiere Beta 7 en cliente y servidor; conserva JSON existentes.
- La Beta 6 fue confirmada por el usuario en Youer, incluidos los nueve botones y el reinicio.

## 0.1.0-beta.6

- Actualiza la acción Gradle de GitHub a v5 (Node.js 24) para eliminar el aviso de Node.js 20.

- La tecla predeterminada para abrir el menú principal ahora es Z, configurable en Controles.
- Conserva los permisos del servidor y las asignaciones de teclas guardadas del jugador.

## 0.1.0-beta.5

- Corrige Gachas, Misiones y GTS en Youer cuando sus comandos no están en el mapa Bukkit.
- Selecciona la vía nativa de Minecraft antes de ejecutar y conserva las mayúsculas del comando.
- Comprueba los requisitos del comando nativo con el jugador y respeta sus permisos.
- Un comando Bukkit registrado mantiene su vía: una denegación, fallo o resultado incierto
  no dispara una segunda ejecución por Minecraft.
- Seis pruebas nuevas para selección de vía, denegaciones y ausencia de reintentos.

## 0.1.0-beta.4

- Dos columnas de botones y paginación adaptada a la altura disponible.
- PC, Fly y Ender Chest, más Gachas, Misiones, GTS y Medallas de los mods Zian.
- Menús JSON del servidor en `config/zian_gui/menus/`, con ejemplo principal automático.
- `/ZianGui open <menú>` y `/ZianGui reload`, restringido por `zian.gui.reload`.
- Validación de campos, comandos, permisos, IDs duplicados y límites de tamaño/cantidad.
- Recarga completa o sin cambios: un archivo inválido conserva la configuración anterior.
- Al recargar se invalidan y cierran las pantallas antiguas sin borrar los límites de clics.
- Los comandos con argumentos se comprueban por su raíz en el puente Bukkit.
- Protocolo 2: instalar esta versión en cliente y servidor; no mezclar con Betas 1–3.
- No incluye ejecución como consola/OP, editor ni acciones encadenadas.

## 0.1.0-beta.3

- Acciones en filas de 30 píxeles con icono y texto alineados, eliminando el espacio vacío de las tarjetas.
- Cerrar usa el mismo fondo oscuro, borde dorado y respuesta al cursor que las acciones.
- Panel más compacto y espacio separado para los avisos de permisos.
- Confirmación del usuario: la curación funciona en Youer al conceder
  `minecraft.command.healpokemon` al grupo `default`.

## 0.1.0-beta.2

- Panel y botones más compactos: tarjetas de 104 × 68 en lugar de 124 × 100.
- Curación comprueba el permiso propio `cobblemon.command.healpokemon.self`
  y el permiso adicional de Youer `minecraft.command.healpokemon`.
- Antes de ejecutar, el puente Bukkit comprueba que el comando exista y permita al jugador
  usarlo; no registra una denegación del wrapper como una ejecución aceptada.
- Ejemplos de LuckPerms para el grupo `default`, sin conceder OP ni comodines.
- Dos pruebas nuevas para los permisos combinados de curación y Spawn.

## 0.1.0-beta.1

- Primera pantalla propia, abierta con `/ZianGui` o `/ZianGui open`.
- Botones Spawn y Curar Pokémon con iconos vanilla y estado bloqueado por permisos.
- Tecla opcional configurable en Controles, sin asignación inicial para evitar conflictos.
- Protocolo obligatorio en cliente y servidor, con límites de longitud y cantidad.
- Comprobación de permisos Bukkit/LuckPerms en Youer al abrir y al pulsar.
- Ejecución como el jugador, sin elevar privilegios ni reintentar una ejecución incierta.
- Sesiones temporales, protección frente a clics repetidos y limpieza al desconectar.
- Pruebas automatizadas para permisos, sesiones y protocolo.
- Guía de validación manual en Youer. Menús JSON y editor quedan para fases posteriores.

## 0.0.1

- Base del mod, compilación y CI.
- Nombre previsto del comando unificado como `/ZianGui`.
