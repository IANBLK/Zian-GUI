# Changelog

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
