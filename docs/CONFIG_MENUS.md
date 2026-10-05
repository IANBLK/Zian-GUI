# Menús configurables — Beta 6

Instalar `zian-gui-0.1.0-beta.6.jar` en servidor y cliente, retirando el JAR anterior.
No requiere cambios del startup. El protocolo cambió: ambos lados necesitan esta versión.

## Archivo del servidor

Al arrancar se crea `config/zian_gui/menus/principal.json` si no hay menús JSON.
Los archivos existentes nunca se sobrescriben. El ejemplo está en
[src/main/resources/defaults/principal.json](../src/main/resources/defaults/principal.json).
El orden de `buttons` determina las posiciones: izquierda, derecha y siguiente fila.
La interfaz tiene dos columnas; cambia de página cuando no cabe todo.

Esta es una implementación parcial del diseño general: solo iconos de items y
comandos como jugador. El formato completo de la especificación futura, las acciones
encadenadas, posiciones manuales, consola, OP temporal y editor todavía no están disponibles.
Los campos no soportados se rechazan para evitar configuraciones que parezcan funcionar.

Ejemplo mínimo:

```json
{
  "id": "principal",
  "title": "Menú principal",
  "permission": "zian.gui.menu.principal",
  "buttons": [
    {
      "id": "spawn",
      "name": "Spawn",
      "icon": "minecraft:compass",
      "permissions": ["eternalcore.spawn"],
      "command": "spawn"
    }
  ]
}
```

- `id`: letras minúsculas, números y guion bajo, máximo 32 caracteres.
- `title` y `name`: texto plano, máximo 64 caracteres; sin códigos de color en esta fase.
- `icon`: ID de item `namespace:nombre`. Un item inexistente usa Barrera sin provocar un cierre.
- `permission`: permiso del menú. Además siempre se requiere `zian.gui.open`.
- `permissions`: todos estos permisos deben estar concedidos. Una lista vacía añade
  ningún permiso del botón, pero el comando de destino sigue comprobando los suyos.
- `command`: comando sin / inicial, hasta 256 caracteres. Admite argumentos.
  No admite saltos de línea, controles, punto y coma ni placeholders.
  Se ejecuta como el jugador: el JSON no concede OP ni evita los permisos del destino.
- Máximo 16 archivos, 24 botones por menú, 8 permisos por botón y 64 KiB por archivo.
- Debe existir un menú con `id: principal`. Para otros menús copia un archivo, cambia
  su ID y permiso y usa `/ZianGui open <id>`.

## Recargar

Editar el archivo del servidor y ejecutar desde consola, sin /:

```text
ZianGui reload
```

En el juego: `/ZianGui reload`, con `zian.gui.reload` concedido al grupo administrativo.
El permiso se respeta mediante LuckPerms incluso para OP. No se necesita concederlo
al grupo default para abrir menús.

Si cualquier archivo está mal, se informa del error y sigue funcionando la configuración
anterior. Si todos son válidos, se aplican juntos y se cierran las pantallas anteriores.
Si la configuración ya está mal al arrancar, los menús quedan desactivados hasta corregirla
y recargar; los archivos del administrador no se sustituyen por un ejemplo.

## Integraciones incluidas

- PC: `pc`, con `cobblemon.command.pc` y `minecraft.command.pc`.
- Fly: `fly`, con `eternalcore.fly`.
- Ender Chest: `enderchest`, con `eternalcore.enderchest`.
- Gachas: `ZianUtilities gacha`, con `zian.gui.button.gacha` y `minecraft.command.ZianUtilities`.
- Misiones: `ZianUtilities quest`, con `zian.gui.button.quests` y `minecraft.command.ZianUtilities`.
- GTS: `ZianGTS`, con `ziangts.open` y `minecraft.command.ZianGTS`. El destino
  también respeta `ziangts.use`.
- Medallas: `medals`, con `zianrct.medals` y `minecraft.command.medals`.

Los permisos adicionales `minecraft.command.*` corresponden al wrapper de Youer.
Los botones solo abren interfaces: no giran el gacha, aceptan misiones, compran Pokémon
ni modifican medallas. El mod de destino continúa aplicando sus reglas.
Si falta un mod/plugin o su comando está desactivado, la GUI no lo ejecutará ni reintentará.
Desde la Beta 5, si el comando no está registrado en Bukkit, se selecciona el registro
nativo de Minecraft antes de ejecutar. Esto permite abrir comandos de mods en Youer.
Un comando Bukkit existente que deniegue permisos o falle no se reintenta por otra vía.

## Prueba en Youer

1. Con una cuenta sin OP y los permisos de grupo del README, probar los nueve botones.
   Abrir PC y Ender Chest debe conservar sus interfaces al cerrarse Zian GUI.
2. Revisar escalas 2 y 3; en ventanas bajas usar las flechas para llegar a todos los botones.
3. Denegar un permiso de un botón al grupo: debe mostrarse bloqueado al reabrir,
   y rechazar el clic si el permiso se retiró mientras el menú estaba abierto.
4. Cambiar el nombre de un botón en el JSON y recargar: debe cambiar al volver a abrir.
5. Introducir un error en el JSON y recargar: debe rechazar la recarga y conservar el menú.
   Corregirlo y recargar de nuevo.
6. Abrir la GUI, cambiar una acción en el JSON y recargar: la pantalla vieja debe cerrarse,
   y sus clics no deben ejecutar la acción nueva.
7. Sin `zian.gui.reload`, una cuenta normal no debe poder recargar.

La compilación y las pruebas de configuración no sustituyen estos tests reales de las
interfaces de los otros mods. El editor dentro del juego queda para la siguiente fase.
