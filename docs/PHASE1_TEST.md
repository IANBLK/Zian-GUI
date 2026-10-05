# Prueba de primera pantalla en Youer

Versión: **0.1.0-beta.2**, Minecraft 1.21.1, Java 21.
Estado: compilación y pruebas automatizadas separadas de la prueba real en Youer.
No se han certificado todavía el dibujo en el juego ni el puente de comandos de tu servidor.

## Preparación

1. Con el cliente y servidor apagados, retirar el JAR anterior de Zian GUI.
2. Instalar `zian-gui-0.1.0-beta.2.jar` en ambos y reiniciar. No cambia el startup.
3. Usar una cuenta sin OP. Desde consola conceder los cinco permisos del README al grupo de esa
   cuenta (`default` en los ejemplos). Verificar que `spawn` y `healpokemon` existen y funcionan manualmente como ella.
4. Esta fase no lee JSON. Solo tiene un menú fijo llamado `principal`.

## Apertura y dibujo

- `/ZianGui` y `/ZianGui open` deben abrir la misma pantalla: Spawn, Curar Pokémon y Cerrar.
- Probar escalas GUI 2 y 3 y cambiar el tamaño de ventana: textos y tarjetas legibles.
- Cerrar con Escape y Cerrar. Asignar una tecla en Controles → Zian GUI y comprobar que abre
  cuando no hay otra pantalla. No sustituye la tecla M de Zian RCT.
- Desde consola `/ZianGui` debe responder que se abre desde el juego.

## Acciones y permisos

- Spawn: cambiar de ubicación, abrir y pulsar; se debe ejecutar `/spawn` como el jugador.
- Curar: con un Pokémon herido, abrir y pulsar; se debe ejecutar `/healpokemon` como el jugador.
- Quitar `eternalcore.spawn`: al reabrir, Spawn debe verse bloqueado y no teletransportar.
- Quitar `cobblemon.command.healpokemon.self`: al reabrir, Curar debe verse bloqueado y no curar.
- Quitar `minecraft.command.healpokemon`: Curar también debe verse bloqueado, aunque
  exista el permiso de Cobblemon. Restaurar ambos para probar la curación.
- El permiso genérico de la Beta 1, `cobblemon.command.healpokemon`, no sustituye a
  los permisos anteriores; no hace falta conceder `.other` para curarse a uno mismo.
- Si Spawn no está configurado, establecerlo con el comando administrativo `/setspawn`
  de EternalCore usando una cuenta con permiso. El botón no configura ni elige el destino.

Fuentes verificadas para estos permisos:
[wrapper de comandos de Youer](https://github.com/MohistMC/Youer/blob/1.21.1/src/main/java/org/bukkit/craftbukkit/command/VanillaCommandWrapper.java)
y [curación de Cobblemon](https://github.com/Cobblemon-Global/Cobblemon/blob/main/common/src/main/kotlin/com/cobblemon/mod/common/command/HealPokemonCommand.kt).

- Con la pantalla ya abierta, denegar el permiso de un botón por consola; pulsarlo debe
  rechazarse aunque se dibujara desbloqueado. Reabrir actualiza su estado visual.
- Denegar `zian.gui.open` o `zian.gui.menu.principal`: apertura y acciones deben rechazarse.
- Repetir una denegación explícita con una cuenta OP: el puente no debe saltársela.
- Restaurar permisos. Cambios de LuckPerms pueden necesitar su propagación habitual.

## Sesiones y fallos

- Pulsar rápidamente varias veces una acción: solo debe ejecutarse una vez; el menú se cierra.
- Esperar más de cinco minutos con el menú abierto: el clic antiguo no debe ejecutar nada;
  cerrar y abrir de nuevo para continuar.
- Desconectar y reconectar: solo una pantalla abierta de nuevo tendrá sesión válida.
- Si un comando no existe o falla, revisar el mensaje del propio comando y el log:
  Zian GUI no debe intentarlo por una segunda ruta ni conceder OP.
- El log `DISPATCHED` significa que el despachador aceptó el comando, no certifica el
  efecto del plugin; comprobar el teletransporte o la curación en el juego.

## Criterio de salida

Registrar versión Youer, resultados de apertura, ambos botones, revocación de permisos,
escalas 2/3 y ausencia de errores. Con esa confirmación se podrá integrar el PR de `dev`
y avanzar a menús configurables. Esta pantalla no requiere repetir las pruebas de misiones
y gachas de los otros mods.
