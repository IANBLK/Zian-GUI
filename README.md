# Zian GUI

Pantalla propia para Minecraft 1.21.1 con NeoForge, diseñada para servidores Youer.
Autor: IANBLK. Licencia MIT.

## 0.1.0-beta.5 — menús configurables

`/ZianGui` y `/ZianGui open` abren el menú principal con nueve botones:
**Spawn, Curar Pokémon, PC, Ender Chest, Fly, Gachas, Misiones, GTS y Medallas**.
La pantalla usa dos columnas compactas y páginas cuando no cabe todo,
bordes dorados e iconos de Minecraft. Cerrar comparte el mismo estilo visual.
Los botones sin permiso se muestran bloqueados.
En Controles se puede asignar una tecla para abrirla; inicialmente no tiene tecla asignada.

Los menús JSON se crean en `config/zian_gui/menus/` en el servidor.
`/ZianGui open <menú>` abre otro menú y `/ZianGui reload` aplica cambios sin reiniciar.
Todavía no incluye editor, `list`, `edit`, `check` ni ejecución como consola u OP.
Las integraciones usan `/ZianUtilities gacha`, `/ZianUtilities quest`, `/ZianGTS` y `/medals`.
La raíz `/zgui` no se registra.

## Instalación

- Minecraft 1.21.1, Java 21 y NeoForge 21.1.228 o posterior de la rama 21.1.
- Instalar **el mismo JAR** en `mods/` del cliente y del servidor y reiniciar ambos.
- El protocolo de red es obligatorio en ambos lados.
- LuckPerms no es una dependencia obligatoria. En Youer se consulta el permiso Bukkit
  del jugador, compatible con LuckPerms instalado como plugin.
- El mod no requiere Cobblemon ni EternalCore para cargar. Para que sus botones hagan
  algo, el servidor sí necesita proporcionar sus comandos. Las pantallas de Cobblemon
  y los otros mods Zian necesitan esos mods también en el cliente.

## Permisos en Youer

Para abrir: `zian.gui.open` y `zian.gui.menu.principal`.
Para Spawn: `eternalcore.spawn`. Para curar: `cobblemon.command.healpokemon.self` y
`minecraft.command.healpokemon` (permiso que añade Youer al envolver el comando del mod).

Ejemplo desde consola para todos los jugadores del grupo `default`:

Lista completa para restaurar estos accesos desde cero:
[permissions-default.txt](docs/permissions-default.txt).

```text
lp group default permission set zian.gui.open true
lp group default permission set zian.gui.menu.principal true
lp group default permission set eternalcore.spawn true
lp group default permission set cobblemon.command.healpokemon.self true
lp group default permission set minecraft.command.healpokemon true
lp group default permission set cobblemon.command.pc true
lp group default permission set minecraft.command.pc true
lp group default permission set eternalcore.enderchest true
lp group default permission set eternalcore.fly true
lp group default permission set zian.gui.button.gacha true
lp group default permission set zian.gui.button.quests true
lp group default permission set minecraft.command.ZianUtilities true
lp group default permission set ziangts.use true
lp group default permission set ziangts.open true
lp group default permission set minecraft.command.ZianGTS true
lp group default permission set zianrct.medals true
lp group default permission set minecraft.command.medals true
```

El comando de destino también puede requerir otros permisos propios del servidor.
Los ejemplos habilitan todos los botones para las pruebas; concede a cada grupo solo los
botones que quieras ofrecer. No concedas `zian.gui.reload` al grupo de jugadores:
la recarga se realiza desde consola o con ese permiso en el grupo administrativo.
Comprueba primero que cada comando funcione manualmente con esa cuenta.
En NeoForge puro, sin Bukkit, esta primera fase permite acciones protegidas solo a OP
de nivel 2 o superior; todavía no integra un proveedor de permisos de NeoForge.

## Seguridad

Los comandos y permisos del JSON se quedan en el servidor. El cliente solo envía identificadores
y una sesión temporal. El servidor vuelve a comprobar permisos al pulsar, limita las
solicitudes, descarta sesiones antiguas y consume la sesión antes de ejecutar una acción.
Las sesiones caducan a los cinco minutos y se limpian al desconectar.

Los comandos se ejecutan como el jugador, sin conceder OP ni usar la consola.
Una denegación explícita de permisos se respeta incluso para OP. Si el puente Bukkit falla,
se deniega la acción; una ejecución incierta nunca se reintenta automáticamente.

La recarga valida todos los menús antes de sustituir la configuración. Un error conserva
la configuración anterior; una recarga válida cierra e invalida las pantallas antiguas.
Consulta el [formato y las pruebas de configuración](docs/CONFIG_MENUS.md).

## Descargar y probar

[Actions](https://github.com/IANBLK/Zian-GUI/actions): abrir la ejecución verde de `dev`,
descargar el artifact `zian-gui-<commit>` y extraer **`zian-gui-0.1.0-beta.5.jar`**.
Los artifacts duran 14 días y requieren iniciar sesión en GitHub.

Las pruebas automatizadas cubren permisos, sesiones y límites del protocolo.
La validación visual y de los comandos en Youer requiere la prueba real del servidor;
una compilación correcta no la certifica. Seguir [la guía de prueba](docs/PHASE1_TEST.md).

## Desarrollo

Compilar: `./gradlew build --no-daemon` (Linux), `gradlew.bat build --no-daemon` (Windows).
El JAR jugable queda en `build/libs/`.
Trabajar en `dev`; integrar a `main` por PR después de probar en Youer.

- [Cambios](CHANGELOG.md)
- [Especificación](docs/ESPECIFICACION_ZIAN_GUI.md)
- [Guía de desarrollo](docs/GUIA_DE_PASOS_PARA_CHATGPT.md)
- [Referencia visual Zian RCT](docs/ANALISIS_ZIANRCT_UI.md)
