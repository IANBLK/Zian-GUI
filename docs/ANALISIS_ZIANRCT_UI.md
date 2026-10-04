# Análisis de la UI de ZianRCT

Revisión de solo lectura del commit `1400f3c7dfb3e5c005e6301428ca6b70ec05edbe`
de [IANBLK/Zian-RCT](https://github.com/IANBLK/Zian-RCT).
No se ha modificado ese repositorio.

## Clases y reutilización

| Ruta en Zian-RCT | Función | Adaptación propuesta |
|---|---|---|
| `src/main/java/com/ianblk/zianrct/client/MedalCaseScreen.java` | Screen propio, panel, grilla, tarjetas, tooltips, comprobación de texturas | Adaptar panel, grilla, hover y fallback a botones de menú |
| `src/main/java/com/ianblk/zianrct/client/ZianRctClient.java` | Registro del keybind, tick del cliente, apertura y limpieza al salir | Reutilizar el patrón de eventos; solicitar datos al servidor antes de abrir |
| `src/main/java/com/ianblk/zianrct/client/ClientMedalState.java` | Estado de snapshot y solicitudes de apertura | Crear un estado propio para menús, sin lógica de medallas |
| `src/main/java/com/ianblk/zianrct/network/ZianRctNetwork.java` | Registro de payloads y envío de snapshots | Reutilizar el patrón de registrar y encolar trabajo, aislando los handlers de cliente |
| `src/main/java/com/ianblk/zianrct/network/MedalOpenPayload.java` | Payload con TYPE y StreamCodec | Referencia para tipos y codecs, con límites propios |

No se encontraron una Screen base compartida, widgets de botón independientes,
nine-slice ni scroll reutilizable. La Screen dibuja directamente las tarjetas;
no son botones clicables. No trasladar lógica de premios, entrenadores o perfiles.
La pantalla no pausa el juego y sobrescribe renderBackground para dibujar su
fondo sin el blur de Screen.

## Tema y recursos

Raíz: `src/main/resources/assets/zianrct/textures/gui/`.

| Archivo | Tamaño real | Uso |
|---|---|---|
| `slot_obtained.png` | 64 × 72 | Tarjeta habilitada |
| `slot_locked.png` | 64 × 72 | Tarjeta bloqueada |
| `slot_hover.png` | 64 × 72 | Capa de hover |
| `lock.png` | 10 × 12 | Candado superpuesto |
| `medals/*.png` | Ver cada recurso | Medallas específicas; no son iconos genéricos de comandos |

La Screen usa tarjetas de 64 × 72, separación de 10 y medalla renderizada a
38 × 38. El tamaño renderizado de la medalla no obliga a crear nuevos iconos de
38 px: usar los 32 × 32 especificados para Zian GUI y escalarlos dentro de la tarjeta.
Fuente: `Screen.font`, fuente predeterminada de Minecraft; no se registra una propia.

Colores del dibujo de respaldo: fondo externo `0x90000000`, panel
`0xE0181818`, borde superior/izquierdo `0xFF555555`, inferior/derecho
`0xFF333333`; tarjeta `0xC02B2B2B`, hover `0xD0444444`, borde habilitado
`0xFFF2C14E`, bloqueado `0xFF666666`. Etiqueta blanca o gris `0x999999`.
Los tooltips se dibujan con `GuiGraphics.renderTooltip` al final del render.
La comprobación de textura abre el recurso y lo decodifica con NativeImage;
cachea el resultado y recurre a formas si falta o falla.

## Networking y aislamiento

ZianRCT usa `CustomPacketPayload`, `StreamCodec`, `RegisterPayloadHandlersEvent`,
`event.registrar(MedalProtocol.NETWORK_VERSION)`, `playToClient`,
`context.enqueueWork` y `PacketDistributor.sendToPlayer`.
Su constructor condiciona el inicio de `ZianRctClient` a `Dist.CLIENT`.
Para Zian GUI, las clases de Minecraft cliente y handlers se mantendrán aislados
por distribución; no se copiarán importaciones cliente a la lógica del servidor.
Los payloads normales llevarán IDs y vistas, nunca comandos ni nodos de permiso.

## Decisión de implementación

Adaptar el patrón de dibujo dentro de Zian GUI, con paquete y recursos propios.
Es la opción recomendada por la especificación y adoptada bajo la autorización
de Ian de continuar con criterio propio. No crear `zian-ui-core` todavía:
ZianRCT no ofrece una API de widgets desacoplada y extraerla obligaría a cambiar
otros proyectos sin una necesidad actual. Ventaja de copiar/adaptar: cambios
locales, sin dependencia dura; coste: mantener los ajustes visuales en cada mod.
Una biblioteca futura permitiría compartir correcciones cuando haya tres
consumidores y una API estable, a costa de versionado y despliegue adicionales.

La licencia de ZianRCT es MIT. Si se copia código o recursos, conservar la
atribución y aviso MIT correspondientes. Los iconos nuevos serán originales.

## Teclas

ZianRCT asigna `M` al medallero. En el código revisado de Zian GTS no se
localizó otro registro GLFW/KeyMapping. No se ha inspeccionado el código actual
de Cobblemon ni los controles del modpack completo; no se puede certificar una
tecla libre a partir de estos dos repositorios.

Decisión segura para la primera pantalla: keybind configurable inicialmente
sin asignar (`InputConstants.UNKNOWN`), y apertura mediante `/zgui open`.
Ian podrá asignar una tecla libre en sus controles sin colisionar por defecto.
Esto es una desviación deliberada de la preferencia por una tecla asignada:
queda pendiente elegirla después de verificar los controles reales de Cobblemon
y del modpack, sin inventar esa comprobación.

## Base de compilación

Referencia GTS: commit `51a00fd461da2f68626702abc872b0225e327d5b`,
`build.gradle.kts` (ModDevGradle 2.0.107), `gradle.properties`
(NeoForge 21.1.228), wrapper Gradle 8.11. Se copiaron los archivos del wrapper,
no código de negocio ni dependencias Cobblemon/Kotlin.
ZianRCT usa NeoForge 21.1.252 y ModDevGradle 2.0.147: no se mezclaron sus
versiones con las de la base solicitada desde GTS.

## Verificación pendiente

La base 0.0.1 requiere comprobar `[ZianGUI] cargado` en cliente y Youer.
La fase de pantalla requiere probar permisos con LuckPerms y la ejecución real
de `/spawn` y `/healpokemon` en Youer. Una compilación verde no demuestra esos
comportamientos. Se conservará la base en dev hasta tener esa evidencia.
