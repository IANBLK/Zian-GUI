# Zian GUI — Especificación técnica

Documento de traspaso: define **qué** construir y **cómo estructurarlo**. La programación del mod y la creación de iconos la hace quien implemente (ChatGPT con acceso a GitHub). Si algo de aquí choca con lo que se ve en el código real de los repos, **avisar antes de improvisar**.

---

## 1. Objetivo

Mod con una **pantalla GUI propia (client-side Screen)**, personalizable por archivos de configuración, donde el admin define botones con nombre e icono. Cada botón ejecuta uno o más comandos (de mods o plugins) y solo se muestra/habilita si el jugador tiene el permiso (LuckPerms).

Ejemplos de botones: `/pc`, `/healpokemon` (Cobblemon); `/spawn`, `/warp`, `/rtp`, `/enderchest`, `/fly`, `/feed`, `/home` (EternalCore); y botones que abran pantallas/comandos de Zian GTS y Zian Utilities.

**Principio clave: ningún botón depende de que exista un plugin o mod concreto.** Cada botón es: *nombre + icono + permiso opcional + lista de comandos libres*, y esos comandos pueden venir de un mod, de un plugin o ser **vanilla puro**. Ejemplo: un botón llamado «Nether» que ejecute `/execute in minecraft:the_nether run tp @s 0 150 0`. Si el servidor no tiene `/spawn` ni `/warp`, el admin simplemente crea sus propios botones con comandos vanilla. Los botones se pueden crear editando el JSON **o desde un editor dentro del juego** (solo admins).

## 2. Decisiones ya tomadas

| Tema | Decisión |
|---|---|
| Plataforma | NeoForge **1.21.1**, Java 21, servidor **Youer** (híbrido NeoForge + Bukkit) |
| Tipo de GUI | **Pantalla propia (`Screen`)**, NO inventario tipo cofre |
| Estilo visual | Reutilizar la interfaz/estilo creada para **ZianRCT** (ver paso 0) |
| Instalación | Mod **obligatorio en servidor y cliente** (`side="BOTH"`) |
| Autoridad | **El servidor decide todo.** El cliente solo dibuja y reporta clics |
| Permisos | LuckPerms, consultado desde el servidor |
| Config | Archivos JSON en `config/zian_gui/menus/`, recargables con `/ZianGui reload` |

## 3. Paso 0 (antes de escribir código nuevo)

Inspeccionar el repo de **ZianRCT** (y Zian Utilities si aplica) y extraer:

1. Clases base de pantalla/UI (Screen base, widgets de botón, paneles, tooltips, scroll).
2. Texturas y tema (colores, fuentes, nine-slice de paneles/botones).
3. Cómo registran networking (payloads) y keybinds, para mantener el mismo patrón.

Decisión a documentar tras la revisión: **copiar** esas clases dentro de Zian GUI, o **extraerlas a una librería compartida** (`zian-ui-core`) usada por todos los mods Zian. Recomendación: empezar copiando (más rápido) y extraer a librería cuando haya 3 mods que la usen.

## 4. Arquitectura

```
Cliente                                   Servidor (Youer)
-------                                   ----------------
Keybind / /ZianGui open ───────────────────► RequestOpenPayload(menuId)
                                          · valida permiso del menú
                                          · filtra botones por permiso
◄─────────────── OpenMenuPayload(layout + botones visibles)
ZianGuiScreen dibuja
Clic en botón ──────────────────────────► ClickButtonPayload(menuId, buttonId)
                                          · valida sesión, permiso, cooldown
                                          · ejecuta acciones
◄─────────────── (opcional) Close/Refresh/Feedback
```

Principios de seguridad (obligatorios):

- El cliente **nunca recibe** comandos ni permisos; solo recibe lo que puede ver: id, etiqueta, icono, tooltip, estado (`ENABLED` / `LOCKED`).
- El cliente **nunca envía** comandos; solo `menuId` + `buttonId`.
- El servidor mantiene una **sesión de menú abierto por jugador** (qué menú tiene abierto). Un `ClickButtonPayload` de un menú que no está abierto se ignora.
- El servidor **revalida el permiso en cada clic** (puede haber cambiado) y aplica cooldown.
- Límite de tasa de clics por jugador y límites de longitud en strings de los payloads.
- Limpiar sesión y cooldowns al desconectarse.
- **El texto de un comando solo puede venir de los JSON del servidor**, nunca de un clic normal. La única excepción es el editor en juego (sección 7.1), que exige permiso de admin comprobado en el servidor y deja registro en el log de cada cambio.

## 5. Networking (NeoForge 1.21.1)

Usar `CustomPacketPayload` + `StreamCodec`, registrados en `RegisterPayloadHandlersEvent` con `registrar("1")` (versión de protocolo; subirla cuando cambie el formato).

| Payload | Dirección | Contenido |
|---|---|---|
| `RequestOpenPayload` | C→S | `menuId` (vacío = menú por defecto) |
| `OpenMenuPayload` | S→C | `menuId`, `title`, `columns`, lista de `ButtonView` |
| `ClickButtonPayload` | C→S | `menuId`, `buttonId` |
| `CloseMenuPayload` | S→C | (sin datos) cierra la pantalla |
| `FeedbackPayload` | S→C | texto corto (ej. "Espera 5 s") para mostrar en la pantalla |
| `EditButtonPayload` | C→S | `menuId` + definición completa del botón (nombre, icono, permiso, acciones, posición). **Solo admins** |
| `DeleteButtonPayload` | C→S | `menuId`, `buttonId`. **Solo admins** |
| `EditorResultPayload` | S→C | OK o lista de errores de validación para mostrar en el editor |

`ButtonView`: `id`, `label`, `iconRef`, `tooltipLines`, `state` (`ENABLED`/`LOCKED`), `position` (columna/fila o slot de la grilla). Para un botón `LOCKED`, `tooltipLines` ya incluye la línea de bloqueo; el **mensaje de acceso denegado completo lo envía el servidor al hacer clic** (vía `FeedbackPayload` y chat), no se manda por adelantado.

Las clases de cliente (`Screen`, keybinds, renderizado) deben ir aisladas con `Dist.CLIENT` / `@EventBusSubscriber(value = Dist.CLIENT)` para que **no se carguen en el servidor dedicado**.

## 6. Estructura del proyecto

```
zian-gui/
├─ build.gradle · gradle.properties · settings.gradle   (ModDevGradle, igual que los otros mods Zian)
├─ gradlew · gradlew.bat · gradle/wrapper/              (obligatorio para que compile el CI)
├─ .github/workflows/build.yml                          (CI, ver sección 14)
├─ LICENSE (MIT) · README.md · .gitignore
├─ docs/ESPECIFICACION_ZIAN_GUI.md                      (copia de este documento)
└─ src/main/
   ├─ java/com/ianblk/ziangui/
   │  ├─ ZianGui.java                  entrada común
   │  ├─ common/
   │  │   ├─ config/   MenuLoader, Models (MenuDef, ButtonDef, IconDef, ActionDef)
   │  │   ├─ net/      payloads + registro (NetworkHandler)
   │  │   └─ util/     LegacyText (&a, &l, &#RRGGBB) para etiquetas
   │  ├─ server/
   │  │   ├─ MenuManager.java          registro de menús, sesiones, cooldowns
   │  │   ├─ PermissionService.java    consulta de permisos
   │  │   ├─ CommandBridge.java        ejecución de comandos
   │  │   ├─ ActionRunner.java         player_command, console_command, open_menu, message, sound, close
   │  │   └─ ZianGuiCommand.java       /ZianGui open|reload|list  y alias /menu
   │  ├─ client/
   │  │   ├─ ClientSetup.java          keybind, registro de pantalla
   │  │   ├─ ZianGuiScreen.java        pantalla (usa la UI de ZianRCT)
   │  │   └─ ui/                       widgets reutilizados/adaptados
   │  └─ api/ ZianGuiApi.java          para que otros mods Zian registren acciones/menús
   └─ resources/
      ├─ META-INF/neoforge.mods.toml
      ├─ defaults/principal.json       menú de ejemplo que se copia a config si no hay menús
      └─ assets/ziangui/
          ├─ textures/gui/             fondo, botones, marcos
          ├─ textures/gui/icons/       iconos (ver sección 9)
          └─ lang/es_es.json · en_us.json
```

## 7. Formato de configuración

Archivo: `config/zian_gui/menus/principal.json` (un archivo por menú; si no hay ninguno, se copia el de ejemplo).

```json
{
  "id": "principal",
  "title": "&6&lMenú Principal",
  "columns": 4,
  "open_permission": "",
  "buttons": [
    {
      "id": "spawn",
      "position": { "column": 0, "row": 0 },
      "icon": "ziangui:spawn",
      "name": "&aSpawn",
      "lore": ["&7Teletransporte al spawn"],
      "permission": "eternalcore.spawn",
      "show_when_denied": "locked",
      "denied_message": "&cNo tienes permisos para usar este botón. Necesitas un rango activo con acceso a /spawn.",
      "cooldown_seconds": 3,
      "actions": [
        { "type": "player_command", "command": "spawn" },
        { "type": "close" }
      ]
    },
    {
      "id": "healpokemon",
      "position": { "column": 1, "row": 0 },
      "icon": "ziangui:heal",
      "name": "&cCurar Pokémon",
      "lore": ["&7Cura a tu equipo"],
      "permission": "cobblemon.command.healpokemon.self",
      "show_when_denied": "hidden",
      "cooldown_seconds": 30,
      "actions": [
        { "type": "player_command", "command": "healpokemon" },
        { "type": "message", "text": "&aTu equipo fue curado." }
      ]
    },
    {
      "id": "gts",
      "position": { "column": 2, "row": 0 },
      "icon": "item:minecraft:emerald",
      "name": "&bGTS",
      "actions": [
        { "type": "open_menu", "menu": "gts" }
      ]
    },
    {
      "id": "nether",
      "position": { "column": 3, "row": 0 },
      "icon": "item:minecraft:netherrack",
      "name": "&cNether",
      "lore": ["&7Viaja a la zona del Nether"],
      "permission": "",
      "cooldown_seconds": 10,
      "actions": [
        {
          "type": "command",
          "as": "player_op",
          "command": "execute in minecraft:the_nether run tp @s 0 150 0"
        },
        { "type": "message", "text": "&cBienvenido al Nether." },
        { "type": "close" }
      ]
    }
  ]
}
```

Reglas:

- `icon`: `modid:nombre` → textura en `assets/<modid>/textures/gui/icons/<nombre>.png`; `item:minecraft:xxx` → se dibuja el item vanilla/mod como respaldo. Si falta la textura, usar un icono "desconocido" y registrar un warning (nunca crashear).
- `permission` vacío = siempre disponible. `show_when_denied`: `hidden` (no se envía al cliente) o `locked` (se envía como `LOCKED`, con candado y tooltip). **Por defecto `locked`**, para que el jugador vea el botón y el mensaje de acceso denegado. Todo el detalle de permisos y mensajes está en la sección 7.2.
- Acciones soportadas en v1: `command` (genérica, con campo `as`, ver 7.1), `open_menu`, `message`, `sound`, `close`. Compatibilidad: `player_command` y `console_command` se aceptan como alias de `command` con `as: "player"` y `as: "console"`.
- Campos opcionales por acción: `delay_ticks` (esperar antes de ejecutar, para encadenar comandos), `dispatch` (`auto`/`bukkit`/`vanilla`). Placeholders en comandos: `{player}`, `{uuid}`, `{x}`, `{y}`, `{z}`, `{dimension}`. La `/` inicial del comando es opcional.
- Validar al cargar: ids duplicados, posiciones fuera de la grilla, acciones desconocidas. Un archivo roto se omite con error claro en log; no tumba el servidor.
- Permisos propios del mod: `zian.gui.open`, `zian.gui.open.others`, `zian.gui.reload`, `zian.gui.menu.<id>`.

### 7.1 Botones personalizados: comandos libres y editor en juego

**Cómo se ejecuta cada comando (`as`).** Esto es lo que permite usar comandos vanilla aunque el jugador no sea op:

| `as` | Quién ejecuta | Cuándo usarlo |
|---|---|---|
| `player` | El jugador, con **sus propios permisos** (ruta Bukkit/auto) | Comandos de plugins y mods que ya validan permisos: `/spawn`, `/pc`, `/healpokemon`, `/warp`... |
| `player_op` | El jugador (`@s`, su posición y su dimensión), con **nivel de permiso elevado solo durante esa ejecución** (ruta vanilla/Brigadier, `CommandSourceStack.withPermission(...)`) | Comandos vanilla que un jugador normal no puede usar: `/execute`, `/tp`, `/give`, `/effect`... |
| `console` | La consola del servidor | Comandos de plugin que exigen consola, o dar cosas con `{player}` |

Por qué hace falta `player_op`: un `/execute in minecraft:the_nether run tp @s 0 150 0` lanzado como jugador normal falla por falta de nivel de op. Con `player_op` el comando se ejecuta como el jugador pero con permiso elevado; con `console` no existe `@s`, así que habría que escribirlo como `execute as {player} run ...`.

Reglas y avisos:

- Usar `@s` y no `@p` en el ejemplo del usuario: `@p` apunta al jugador más cercano a quien ejecuta, que podría ser otro. Con `player_op`, `@s` siempre es quien pulsó el botón.
- `player_op` es una **escalada de privilegios controlada**. Es segura únicamente porque el texto del comando sale del JSON del admin y nunca del cliente. Un comando como `execute as @a run ...` afectaría a todos: es responsabilidad del admin que lo define. Documentarlo en el README y registrar en el log cada ejecución de `player_op` y `console` (jugador, botón, comando).
- Los comandos de plugins (EternalCore, etc.) no están garantizados por la ruta `player_op` (vanilla); usar `player` o `console` para esos.
- Al cargar el JSON, validar que `command` no esté vacío y, para `player_op`, intentar parsearlo con el dispatcher vanilla y **avisar por log** si no parsea. Para comandos de plugin no se puede validar: no rechazarlos, solo avisar.
- Un botón puede encadenar varios comandos (lista de acciones con `delay_ticks`).

**Editor en juego (solo admins).**

- Se abre desde la propia pantalla (botón "Editar", visible solo con el permiso `zian.gui.edit` u op 4) o con `/ZianGui edit <menú>`.
- Campos: nombre, icono (selector con los iconos del mod y búsqueda de items), permiso (opcional), posición en la grilla, cooldown, y lista de comandos con selector `as` (`player` / `player_op` / `console`).
- Flujo: el cliente envía `EditButtonPayload` → el servidor **verifica el permiso de admin**, valida (ids únicos, posición libre, longitud de textos, `as` válido) → escribe el JSON → recarga el menú → responde con `EditorResultPayload`. El cliente nunca escribe archivos.
- Cada alta, edición o borrado se registra en el log con el nombre del admin.
- El JSON sigue siendo la fuente de verdad: lo creado en el editor es editable a mano y viceversa.

### 7.2 Permisos con LuckPerms y mensaje de acceso denegado

**Cómo se resuelve el acceso.** El campo `permission` de un botón es un **nodo de LuckPerms**. El servidor consulta `hasPermission(nodo)` del jugador (ver sección 8), así que LuckPerms aplica por sí solo la herencia entre rangos, comodines (`eternalcore.*`), negaciones explícitas, contextos (servidor/mundo) y rangos temporales. Un rango «activo» es simplemente uno que hoy concede el nodo; si el rango caduca o se retira, el botón pasa a bloqueado sin tocar el mod.

Variantes de condición por botón (todas opcionales; si hay varias, deben cumplirse todas):

| Campo | Significado |
|---|---|
| `permission` | Un nodo. Ej. `cobblemon.command.pc` |
| `permissions_any` | Lista; basta con tener **uno** |
| `permissions_all` | Lista; hay que tener **todos** |
| `group` | Atajo para exigir un rango concreto: equivale al nodo `group.<nombre>` de LuckPerms (verificar en Youer, ver preguntas abiertas) |

**Estados y mensajes.**

- `show_when_denied`: `locked` (por defecto) o `hidden`.
- `locked`: el icono se dibuja atenuado con el candado encima y un tooltip como «&cBloqueado · requiere un rango con acceso». Al hacer clic, el servidor responde con el **mensaje de acceso denegado** (chat + aviso dentro de la pantalla) y, opcionalmente, un sonido (`denied_sound`).
- `denied_message` (por botón, opcional). Si falta, se usa el mensaje global de `config/zian_gui/settings.json`:

```json
{
  "default_denied_message": "&cNo tienes permisos para usar este botón. Necesitas un rango activo con acceso.",
  "default_denied_tooltip": "&cBloqueado · requiere un rango con acceso",
  "denied_sound": "minecraft:entity.villager.no",
  "show_permission_in_message": false,
  "fallback_op_level": 2
}
```

- Placeholders disponibles en los mensajes: `{player}`, `{button}`, y (solo si `show_permission_in_message` es `true`) `{permission}`. Por defecto el nodo **no** se muestra a los jugadores.
- Los textos deben poder traducirse (`lang/es_es.json`, `lang/en_us.json`) cuando se usan los valores por defecto del mod.

**Cuándo se comprueba (siempre en el servidor).**

1. Al abrir el menú: decide qué botones se envían y en qué estado.
2. Al hacer clic: se **revalida** (el rango pudo cambiar con la pantalla abierta). Si ahora no tiene acceso → mensaje de acceso denegado y no se ejecuta nada.
3. Opcional (fase 3): refrescar el estado de los botones cada pocos segundos mientras la pantalla está abierta, o al detectar un cambio de rango, para que un botón se desbloquee o bloquee sin reabrir.

**Importante: el permiso del botón y el del comando.**

- `as: "player"`: el comando valida además su propio permiso. Usar en el botón **el mismo nodo que exige el comando**; si no coinciden, el botón saldrá habilitado y luego el plugin responderá con su propio «no tienes permiso».
- `as: "player_op"` y `as: "console"`: el comando **no** vuelve a comprobar permisos del jugador. Aquí el `permission` del botón es la **única barrera**. Si un botón de este tipo no tiene `permission`, es accesible para todos (válido para algo público como el «Nether», pero debe ser una decisión consciente). El cargador debe emitir un aviso informativo en el log cuando vea `console` o `player_op` sin permiso.

**Sin LuckPerms o sin proveedor de permisos.** Orden de resolución en `PermissionService`: (1) `hasPermission` de Bukkit (LuckPerms plugin en Youer) → (2) `PermissionAPI` de NeoForge si existe en la versión usada (verificar) → (3) nivel de op `fallback_op_level` (por defecto 2). Con el respaldo, los botones con permiso quedan solo para ops y los botones sin permiso siguen abiertos a todos.

**Depuración.** Comando `/ZianGui check <jugador> <menú> <botón>` (permiso `zian.gui.reload` u op) que indica qué nodos evaluó, el resultado de cada uno, el estado final del botón y qué proveedor de permisos respondió. Evita adivinar por qué un botón sale bloqueado.

## 8. Permisos y ejecución de comandos en Youer

Puntos delicados, **probar en un Youer real** antes de dar por buena cada decisión:

1. **Consulta de permisos.** Opción recomendada: obtener el jugador Bukkit con `getBukkitEntity()` (por **reflexión**, sin dependencia de compilación contra Bukkit) y llamar `hasPermission(String)`; LuckPerms (plugin) responde ahí. Respaldo si no existe: nivel de op ≥ 2. Alternativa a evaluar si hace falta más control: API de LuckPerms (puede chocar con el aislamiento de classloaders mod/plugin).
2. **Ejecución de comandos.** Modo `auto`: `Bukkit.dispatchCommand(sender, cmd)` por reflexión (sender = jugador Bukkit o consola); si la ruta Bukkit no está disponible o lanza excepción, usar `server.getCommands().performPrefixedCommand(...)`. **No** reintentar por vanilla cuando Bukkit devuelve `false` (riesgo de ejecutar dos veces). Permitir forzar `"dispatch": "bukkit" | "vanilla"` por acción.
3. **Verificar** que la ruta Bukkit ejecuta tanto comandos de plugins (EternalCore) como de mods (Cobblemon). Si no, documentar qué ruta usa cada uno.
4. Los comandos se ejecutan **como el jugador**, de modo que el propio comando vuelve a validar el permiso (el chequeo del botón es solo para mostrar/ocultar, no la única barrera).
5. **Nodos de permiso:** no adivinar. Cobblemon usa `cobblemon.command.<comando>` (ej. `cobblemon.command.pc`, `cobblemon.command.healpokemon.self`). Los de EternalCore hay que **confirmarlos** en su documentación o con `/lp user <jugador> permission info`, y ajustar el JSON de ejemplo.
6. Comandos con argumentos (`/warp <nombre>`, `/home <nombre>`): en v1 el botón lleva el argumento fijo. Listas dinámicas (homes/warps del jugador) quedan para la fase 5.

## 9. Iconos

Formato y reglas:

- PNG con transparencia, **32×32 px** (si el estilo de ZianRCT usa otro tamaño, seguir el de ZianRCT), estilo coherente con el tema de ZianRCT.
- Nombres en `snake_case`, ruta `assets/ziangui/textures/gui/icons/<nombre>.png`.
- Un icono "desconocido" (`unknown.png`) y uno de candado (`locked.png`) son obligatorios.

Lista inicial:

`spawn` · `home` · `sethome` · `warp` · `rtp` · `enderchest` · `fly` · `feed` · `heal` · `pc` (PC Pokémon) · `gts` · `utilities` · `shop` · `back` · `close` · `next_page` · `prev_page` · `locked` · `unknown`

## 10. Apertura del menú

- Keybind configurable en cliente (por defecto una tecla libre; **verificar que no choque** con los keybinds de ZianRCT/Cobblemon).
- Comandos: `/ZianGui open <menú> [jugador]`, `/ZianGui reload`, `/ZianGui list`, alias `/menu`.
- Un botón puede abrir otro menú (`open_menu`), con botón "Atrás" automático opcional.

## 11. Compatibilidad y empaquetado

- `neoforge.mods.toml` con `side="BOTH"`; versión de protocolo de red comprobada (cliente y servidor deben coincidir).
- El mod debe ir en el **modpack del cliente** y en el `mods/` del servidor Youer.
- Sin dependencias duras de otros mods Zian; la integración es vía `ZianGuiApi` o simplemente botones que ejecutan sus comandos.
- Logs con prefijo `[ZianGUI]`; errores de config nunca deben impedir el arranque.

## 12. Hoja de ruta y criterios de aceptación

| Fase | Entregable | Se considera listo cuando |
|---|---|---|
| 0 | Revisión de ZianRCT, decisión copiar vs librería | Hay lista de clases UI a reutilizar |
| 1 | Mod mínimo: keybind → pantalla con 2 botones fijos (`/spawn`, `/healpokemon`) | Funciona en Youer con cliente modeado; el permiso oculta/muestra el botón |
| 2 | Carga desde JSON + `/ZianGui reload` + iconos propios + acción `command` con `as` (`player`/`player_op`/`console`) | Cambiar el JSON y recargar cambia la pantalla sin reiniciar; el botón «Nether» funciona para un jugador sin op |
| 2b | Editor en juego para admins (crear/editar/borrar botones) | Un admin crea un botón con nombre y comando desde la pantalla y funciona al instante; un jugador normal no puede enviar paquetes de edición |
| 3 | Permisos LuckPerms (7.2): estados `locked`/`hidden`, mensaje de acceso denegado, `/ZianGui check`, cooldowns, `open_menu`, feedback en pantalla | Un jugador sin rango con acceso ve el botón bloqueado y recibe el mensaje al pulsarlo; no puede ejecutar el comando ni forzando paquetes; al dar o quitar el rango, el botón cambia de estado |
| 4 | `ZianGuiApi` e integración con Zian GTS / Utilities | Otro mod registra una acción y un botón la usa |
| 5 | Extras: listas dinámicas (homes/warps), paginación, confirmación opcional para botones peligrosos | — |

Pruebas mínimas de la fase 3: jugador sin permiso, jugador con permiso, permiso retirado con el menú abierto, spam de clics, paquete falso con `buttonId` inexistente, desconexión con menú abierto. Pruebas de LuckPerms: permiso heredado de otro rango, comodín (`eternalcore.*`), permiso negado explícitamente, rango temporal que caduca, permiso distinto por contexto (servidor/mundo), botón `player_op`/`console` con y sin `permission`, y servidor sin LuckPerms (respaldo por nivel de op).

## 13. Preguntas abiertas (resolver con el código real)

1. ¿La UI de ZianRCT está en un módulo reutilizable o mezclada con la lógica del mod?
2. ¿Qué tamaño/estilo de iconos y qué fuente usa ZianRCT?
3. ¿Qué tecla es segura para el keybind por defecto?
4. ¿Qué ruta de comandos (Bukkit o vanilla) funciona mejor con EternalCore y Cobblemon en Youer?
5. ¿Los nodos de EternalCore usados en el ejemplo son los correctos?
6. ¿`CommandSourceStack.withPermission(...)` con la ruta vanilla funciona bien en Youer para `/execute` y `/tp` lanzados por un jugador sin op? Probarlo en la fase 2.
7. ¿Qué nivel de permiso elevado conviene para `player_op` (2 basta para `/execute`, `/tp`, `/give`; 4 para comandos de administración)? Recomendación: 2 por defecto y campo opcional `op_level`.
8. ¿LuckPerms está instalado como plugin Bukkit en el Youer de Ian, y `hasPermission` responde bien tanto para nodos normales como para `group.<nombre>`? Confirmarlo con `/ZianGui check` en la fase 3.
9. ¿Los comandos de mods (Cobblemon) consultan LuckPerms por su cuenta o solo usan nivel de op? Si usan op, el botón con `as: "player"` puede salir habilitado y el comando fallar; en ese caso usar `player_op` con el nodo en `permission`.

---

## 14. Flujo de trabajo con GitHub y descarga de builds

**Repositorio oficial:** https://github.com/IANBLK/Zian-GUI (público, licencia MIT). **Todo** lo que se programe o cambie (código, recursos, iconos, documentación, workflow de CI) debe subirse a ese repo. Nada puede quedarse solo en el chat: Ian descargará los builds desde ahí para probarlos.

### 14.1 Reglas de trabajo

1. **Ramas:** trabajar en `dev`; `main` solo recibe lo que ya compila y está probado. Al cerrar cada fase se mergea `dev` → `main` (preferiblemente con Pull Request) y se crea una etiqueta de versión.
2. **Commits:** pequeños y descriptivos, con un solo propósito (ej. `feat: pantalla base con keybind`, `fix: revalidar permiso al hacer clic`). Una fase suele ser varios commits.
3. **Cada push debe compilar.** Tras cada push, comprobar el resultado de GitHub Actions. Si falla, corregir y volver a subir **antes de seguir**, e informar a Ian del error en una frase.
4. **Archivos obligatorios en el repo** (sin ellos el CI no funciona): `gradlew`, `gradlew.bat`, `gradle/wrapper/` (con el `.jar` y `.properties`), `build.gradle`, `settings.gradle`, `gradle.properties`, `.github/workflows/build.yml`, `LICENSE` (MIT), `README.md`. En `.gitignore` añadir además de la plantilla Gradle: `run/`, `.idea/`, `*.iml`, `.vscode/`.
5. **Copiar esta especificación** al repo como `docs/ESPECIFICACION_ZIAN_GUI.md` y mantenerla actualizada si una decisión cambia.
6. **Versionado:** subir `mod_version` en `gradle.properties` al cerrar cada fase: fase 1 → `0.1.0`, fase 2 → `0.2.0`, fase 2b → `0.3.0`, fase 3 → `0.4.0`, fase 4 → `0.5.0`, fase 5 → `1.0.0`. Para pruebas intermedias usar sufijos: `v0.2.0-beta.1`.
7. **Informe al cerrar cada fase** (mensaje corto a Ian): qué se hizo, cómo probarlo, enlace al build (Release o ejecución de Actions), limitaciones conocidas y qué viene después.
8. **Repo público: nunca subir** tokens, contraseñas, IPs/dominios de servidores, datos de jugadores ni configs reales del servidor. Las configs de ejemplo deben ser genéricas.
9. **Iconos y texturas:** subirlos como PNG en `src/main/resources/assets/ziangui/textures/gui/icons/`. Deben ser obra original (la licencia del repo es MIT); no copiar arte de terceros.
10. **README** mínimo: qué es el mod, requisitos (NeoForge 1.21.1, mod en cliente **y** servidor, Youer, LuckPerms opcional), instalación, ejemplo de menú, lista de comandos y permisos, y **cómo descargar las builds** (14.3).

### 14.2 Workflow de CI (archivo `.github/workflows/build.yml`)

Compila con Java 21 en cada push a `main` o `dev`, en cada Pull Request y a mano desde la pestaña Actions. Sube siempre el `.jar` como *artifact* y, cuando se empuja una etiqueta `v*`, publica además un **Release** con el jar adjunto (las etiquetas con guion, como `v0.2.0-beta.1`, se marcan como *pre-release*).

```yaml
name: Build

on:
  push:
    branches: [ main, dev ]
    tags: [ 'v*' ]
  pull_request:
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - name: Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Permisos del wrapper
        run: chmod +x gradlew

      - name: Compilar
        run: ./gradlew build --no-daemon

      - name: Subir jar como artifact
        uses: actions/upload-artifact@v4
        with:
          name: zian-gui-${{ github.sha }}
          path: build/libs/*.jar
          if-no-files-found: error

      - name: Publicar Release (solo en tags v*)
        if: startsWith(github.ref, 'refs/tags/v')
        uses: softprops/action-gh-release@v2
        with:
          files: build/libs/*.jar
          prerelease: ${{ contains(github.ref_name, '-') }}
          generate_release_notes: true
```

Notas para quien lo implemente: si el nombre de la tarea o la ruta del jar cambia (por ejemplo si se generan jars `-sources`), ajustar `path` y `files` para subir **solo** el jar jugable. Si `./gradlew build` ejecuta tests, asegurarse de que no dependan de un servidor Minecraft en ejecución.

### 14.3 Cómo descarga Ian las builds

- **Versiones estables o de fase (recomendado):** pestaña **Releases** del repo → descargar el `.jar` del Release más reciente. No requiere iniciar sesión y es público.
- **Build de cualquier commit (pruebas rápidas):** pestaña **Actions** → abrir la ejecución del commit → sección **Artifacts** → descargar `zian-gui-<commit>`. Descarga un `.zip` que contiene el `.jar` (requiere haber iniciado sesión en GitHub, y los artifacts caducan a los pocos días).
- Para instalarlo: copiar el `.jar` a `mods/` del servidor Youer **y** a la carpeta `mods/` del cliente (el mod es obligatorio en ambos lados).

---

## Prompt sugerido para pasarle a ChatGPT

> Vas a programar el mod **Zian GUI** (NeoForge 1.21.1, Java 21, servidor Youer, cliente y servidor obligatorios). Sigue el documento `ESPECIFICACION_ZIAN_GUI.md` al pie de la letra. Empieza por el **Paso 0**: revisa el repo de ZianRCT, lista qué clases/texturas de UI se pueden reutilizar y propón copiar vs librería compartida. Luego implementa **fase por fase**, entregando **archivos completos** (nunca fragmentos) y esperando mi confirmación al terminar cada fase. Crea también los iconos PNG de la sección 9 con el estilo de ZianRCT. Los botones deben admitir **comandos libres** (vanilla, de mods o de plugins) con los modos `as` de la sección 7.1, incluido `player_op` para que un jugador sin op pueda usar, por ejemplo, un botón «Nether» con `execute in minecraft:the_nether run tp @s 0 150 0`, y debe existir el **editor en juego** para admins (fase 2b). El acceso a cada botón se decide con **LuckPerms** (sección 7.2): si el jugador no tiene un rango activo con acceso, el botón sale bloqueado y, al pulsarlo, recibe un mensaje configurable tipo «No tienes permisos para usar este botón. Necesitas un rango activo con acceso.» **Todo lo que programes o cambies debe subirse al repo https://github.com/IANBLK/Zian-GUI**, siguiendo la sección 14 (rama `dev`, commits pequeños, workflow de GitHub Actions que compile cada push y Releases con el `.jar` al cerrar cada fase), porque descargaré las builds desde ahí para probarlas. Tras cada push revisa que el CI pase; si falla, corrígelo antes de continuar. Si algo de la especificación choca con el código real, pregúntame antes de decidir.
