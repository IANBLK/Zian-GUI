# Zian GUI

Pantalla propia para Minecraft 1.21.1 con NeoForge, diseñada para servidores Youer.
Autor: IANBLK. Licencia MIT.

## 0.1.0-beta.1 — primera pantalla

`/ZianGui` y `/ZianGui open` abren el menú principal con dos botones:
**Spawn** (`spawn`) y **Curar Pokémon** (`healpokemon`). La pantalla usa tarjetas oscuras,
bordes dorados e iconos de Minecraft. Los botones sin permiso se muestran bloqueados.
En Controles se puede asignar una tecla para abrirla; inicialmente no tiene tecla asignada.

Esta fase usa dos acciones fijas. Todavía no incluye menús JSON, editor, `reload`,
`list`, `edit`, `check` ni apertura de las interfaces de otros mods.
Las integraciones futuras conservarán `/ZianUtilities` y `/ZianGTS`.
La raíz `/zgui` no se registra.

## Instalación

- Minecraft 1.21.1, Java 21 y NeoForge 21.1.228 o posterior de la rama 21.1.
- Instalar **el mismo JAR** en `mods/` del cliente y del servidor y reiniciar ambos.
- El protocolo de red es obligatorio en ambos lados.
- LuckPerms no es una dependencia obligatoria. En Youer se consulta el permiso Bukkit
  del jugador, compatible con LuckPerms instalado como plugin.
- El mod no requiere Cobblemon ni EternalCore para cargar. Para que sus botones hagan
  algo, el servidor sí necesita proporcionar los comandos `spawn` y `healpokemon`.

## Permisos en Youer

Para abrir: `zian.gui.open` y `zian.gui.menu.principal`.
Para Spawn: `eternalcore.spawn`. Para curar: `cobblemon.command.healpokemon`.

Ejemplo desde consola para una cuenta de prueba (sustituir `IANBLK`):

```text
lp user IANBLK permission set zian.gui.open true
lp user IANBLK permission set zian.gui.menu.principal true
lp user IANBLK permission set eternalcore.spawn true
lp user IANBLK permission set cobblemon.command.healpokemon true
```

El comando de destino también puede requerir otros permisos propios del servidor.
Comprueba primero que `/spawn` y `/healpokemon` funcionen manualmente con esa cuenta.
En NeoForge puro, sin Bukkit, esta primera fase permite acciones protegidas solo a OP
de nivel 2 o superior; todavía no integra un proveedor de permisos de NeoForge.

## Seguridad

Los comandos y permisos se quedan en el servidor. El cliente solo envía identificadores
y una sesión temporal. El servidor vuelve a comprobar permisos al pulsar, limita las
solicitudes, descarta sesiones antiguas y consume la sesión antes de ejecutar una acción.
Las sesiones caducan a los cinco minutos y se limpian al desconectar.

Los comandos se ejecutan como el jugador, sin conceder OP ni usar la consola.
Una denegación explícita de permisos se respeta incluso para OP. Si el puente Bukkit falla,
se deniega la acción; una ejecución incierta nunca se reintenta automáticamente.

## Descargar y probar

[Actions](https://github.com/IANBLK/Zian-GUI/actions): abrir la ejecución verde de `dev`,
descargar el artifact `zian-gui-<commit>` y extraer **`zian-gui-0.1.0-beta.1.jar`**.
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
