# Zian GUI — Guía de pasos para ChatGPT

Reparto de trabajo:

- **Claude:** diseña el mod (arquitectura, formato de config, seguridad, hoja de ruta) y escribe los pasos que sigue ChatGPT. Revisa el resultado cuando Ian se lo trae.
- **ChatGPT (con acceso a GitHub):** programa el mod, crea los iconos y lo sube a https://github.com/IANBLK/Zian-GUI.
- **Ian:** pega cada paso a ChatGPT, prueba las builds en Youer y trae a Claude lo que haga falta revisar.

Documento de referencia para todo: `docs/ESPECIFICACION_ZIAN_GUI.md` (en el repo). Si algo de esta guía contradice la especificación, **manda la especificación**; avisar a Ian.

---

## Cómo usar esta guía (Ian)

1. Sube al repo `docs/ESPECIFICACION_ZIAN_GUI.md` y `.github/workflows/build.yml`.
2. Pega a ChatGPT el **Prompt de arranque** (una sola vez, al inicio de la conversación).
3. Pega **un paso a la vez**. No pases al siguiente hasta que el CI esté en verde y tú hayas probado lo que indica "Pruebas de Ian".
4. Al terminar cada paso, ChatGPT debe responder con el **formato de informe** de abajo.
5. Trae a Claude el informe (y, si hay un error, el log de Actions) para revisarlo antes de seguir. Claude no tiene acceso a GitHub: revisa lo que le pegues o le describas.

---

## Prompt de arranque (pegar una vez)

```
Vas a programar el mod "Zian GUI" en el repo https://github.com/IANBLK/Zian-GUI.
Stack: NeoForge 1.21.1, Java 21, servidor Youer (híbrido NeoForge + Bukkit), mod
obligatorio en cliente y servidor.

Antes de empezar, lee completo docs/ESPECIFICACION_ZIAN_GUI.md del repo. Esa
especificación es la fuente de verdad. Reglas fijas para todos los pasos:

1. Trabaja en la rama `dev`. `main` solo recibe lo que ya compila y está probado.
2. Entrega archivos COMPLETOS, nunca fragmentos sueltos.
3. Commits pequeños y descriptivos. Después de cada push revisa GitHub Actions;
   si falla, corrígelo antes de seguir y cuéntame el error en una frase.
4. Repo público: nunca subas tokens, contraseñas, IPs/dominios de servidores ni
   datos reales de jugadores. Los iconos deben ser obra original (licencia MIT).
5. Haz SOLO el paso que te pida, sin adelantarte a los siguientes.
6. Si algo de la especificación choca con el código real o no está claro,
   pregúntame antes de decidir.
7. Al terminar cada paso responde con este informe:
   - Qué hiciste (3-6 líneas)
   - Archivos creados/modificados
   - Commits y estado del CI (con enlace)
   - Cómo probarlo (pasos concretos)
   - Problemas, dudas o decisiones que tomaste
   - Enlace al build (Release o ejecución de Actions)

Confírmame que leíste la especificación resumiéndola en 5 líneas y espera mi
primer paso.
```

---

## Paso 1 — Base del repo y CI

**Objetivo:** un mod vacío que compila en GitHub y carga en servidor y cliente.

**Prompt para ChatGPT:**

```
PASO 1: base del repo y CI.
- Crea la estructura de la sección 6 de la especificación: build.gradle
  (ModDevGradle), settings.gradle, gradle.properties (mod_id=ziangui,
  mod_version=0.0.1, mod_license=MIT), wrapper de Gradle (gradlew, gradlew.bat,
  gradle/wrapper/), META-INF/neoforge.mods.toml con side="BOTH", y una clase
  principal ZianGui que solo escriba "[ZianGUI] cargado" en el log.
- Si tienes acceso al repo privado IANBLK/Zian-GTS, usa su build.gradle y
  versión de NeoForge como referencia para que coincidan; si no, usa la
  última 21.1.x estable y dímelo.
- Asegura que existe .github/workflows/build.yml (sección 14.2), LICENSE (MIT),
  README.md básico y .gitignore con las líneas extra de la sección 14.1.
- Copia la especificación a docs/ESPECIFICACION_ZIAN_GUI.md si no está.
- No programes nada de GUI todavía.
```

**Pruebas de Ian:** el CI sale en verde; descargar el jar de Actions; ponerlo en `mods/` del servidor Youer **y** del cliente; arrancar ambos y ver `[ZianGUI] cargado` en los logs.

**Traer a Claude:** nada, salvo que falle.

---

## Paso 2 — Análisis de la UI de ZianRCT (solo lectura)

**Objetivo:** decidir cómo reutilizar la interfaz de ZianRCT antes de escribir la pantalla.

**Prompt para ChatGPT:**

```
PASO 2: análisis de la UI de ZianRCT (no cambies código de ZianRCT).
Revisa el repo de ZianRCT y crea en Zian GUI el archivo
docs/ANALISIS_ZIANRCT_UI.md con:
1. Lista de clases de UI reutilizables (Screen base, widgets de botón, paneles,
   tooltips, scroll) con su ruta y para qué sirven.
2. Texturas y tema: ruta de las texturas, colores, fuente, tamaño de los iconos.
3. Cómo registran networking (payloads) y keybinds, para copiar el patrón.
4. Recomendación: copiar las clases a Zian GUI o extraerlas a una librería
   compartida (zian-ui-core), con pros y contras.
5. Qué teclas ya usa ZianRCT/Cobblemon, para elegir un keybind por defecto libre.
No implementes nada. Sube el documento a `dev` y espera mi decisión.
```

**Pruebas de Ian:** leer el análisis y decidir copiar vs librería (si dudas, trae el documento a Claude).

**Traer a Claude:** `docs/ANALISIS_ZIANRCT_UI.md` para validar la decisión.

---

## Paso 3 — Fase 1: pantalla mínima con red y permisos

**Objetivo:** keybind → pantalla con 2 botones fijos que ejecutan `/spawn` y `/healpokemon`, validados por el servidor. (Spec: secciones 4, 5, 8; fase 1.)

**Prompt para ChatGPT:**

```
PASO 3: Fase 1 de la especificación.
- Aplica la decisión sobre la UI de ZianRCT que te di (copiar / librería).
- Registra los payloads RequestOpenPayload, OpenMenuPayload, ClickButtonPayload,
  CloseMenuPayload y FeedbackPayload (sección 5), con versión de protocolo "1".
- Servidor: MenuManager con sesión de menú abierto por jugador, PermissionService
  (hasPermission de Bukkit por reflexión con getBukkitEntity(); respaldo: op
  nivel 2) y CommandBridge (Bukkit.dispatchCommand por reflexión en modo auto,
  sin reintentar por vanilla si Bukkit devuelve false).
- Cliente: ZianGuiScreen con el estilo de ZianRCT y un keybind configurable
  (tecla por defecto libre, según tu análisis). Aísla todo el código de cliente
  con Dist.CLIENT para que no cargue en el servidor dedicado.
- Menú FIJO en código (aún sin JSON) con 2 botones: Spawn (/spawn, permiso
  eternalcore.spawn) y Curar Pokémon (/healpokemon, permiso
  cobblemon.command.healpokemon.self). El servidor oculta el botón si no hay permiso.
- Seguridad de la sección 4: el cliente solo envía menuId+buttonId, el servidor
  revalida el permiso en cada clic, limpia sesión al desconectarse.
- mod_version=0.1.0. Al terminar: merge dev→main por Pull Request, etiqueta
  v0.1.0 y verifica que el Release trae el jar.
```

**Pruebas de Ian (Youer + cliente con el mod):**

- Con un jugador con permiso: se ve el botón y el comando se ejecuta.
- Con un jugador sin permiso: el botón no aparece.
- Quitar el permiso con la pantalla abierta y pulsar: no se ejecuta nada.
- Anotar si `Bukkit.dispatchCommand` ejecutó bien `/spawn` (plugin) **y** `/healpokemon` (mod). Si alguno falla, apuntar cuál.

**Traer a Claude:** el informe de ChatGPT y, si algún comando no se ejecuta, qué mensaje sale en el log (decide si cambia la ruta de ejecución).

---

## Paso 4 — Fase 2: JSON, comandos libres e iconos

**Objetivo:** menús definidos por archivos, botones con comandos vanilla/mod/plugin y iconos propios. (Spec: secciones 7, 7.1, 9.)

**Prompt para ChatGPT:**

```
PASO 4: Fase 2 de la especificación.
- Carga de menús desde config/zian_gui/menus/*.json (formato de la sección 7),
  con defaults/principal.json copiado si no hay menús, validación (ids
  duplicados, posiciones fuera de la grilla, acciones desconocidas) y errores
  claros en log sin tumbar el servidor. Comando /ZianGui reload, /ZianGui list,
  /ZianGui open <menu> [jugador] y alias /menu.
- Acción genérica "command" con el campo "as": player | player_op | console
  (sección 7.1), más open_menu, message, sound, close; alias player_command y
  console_command; delay_ticks; placeholders {player} {uuid} {x} {y} {z}
  {dimension}. player_op ejecuta por la ruta vanilla con permiso elevado solo
  durante esa ejecución (por defecto nivel 2, campo opcional op_level). Registra
  en el log cada ejecución de player_op y console.
- Incluye en el menú de ejemplo el botón "Nether":
  execute in minecraft:the_nether run tp @s 0 150 0  con as=player_op.
- Crea los iconos PNG de la sección 9 (32x32 o el tamaño de ZianRCT, estilo
  coherente, transparencia), incl. locked.png y unknown.png. Si falta una
  textura: icono "unknown" + warning, nunca crash. Soporta icon "item:minecraft:xxx".
- mod_version=0.2.0, PR a main, etiqueta v0.2.0.
```

**Pruebas de Ian:**

- Editar el JSON, `/ZianGui reload` y ver el cambio sin reiniciar.
- El botón «Nether» funciona con un jugador **sin op** y lo deja en el Nether.
- Un botón con `as: console` y otro con `as: player` funcionan.
- Un JSON roto no impide arrancar el servidor y deja un error claro en el log.

**Traer a Claude:** si `player_op` falla en Youer (mensaje del log) para decidir el ajuste.

---

## Paso 5 — Fase 2b: editor en juego

**Objetivo:** que un admin cree y edite botones desde la pantalla. (Spec: sección 7.1.)

**Prompt para ChatGPT:**

```
PASO 5: Fase 2b de la especificación (editor en juego, solo admins).
- Botón "Editar" visible solo con permiso zian.gui.edit u op 4, y /ZianGui edit <menu>.
- Formulario: nombre, icono (selector con los iconos del mod y búsqueda de items),
  permiso opcional, posición en la grilla, cooldown, lista de comandos con
  selector "as" (player/player_op/console).
- Payloads EditButtonPayload, DeleteButtonPayload y EditorResultPayload. El
  servidor VERIFICA el permiso de admin en cada paquete, valida (ids únicos,
  posición libre, longitudes, "as" válido), escribe el JSON, recarga el menú y
  responde con errores o OK. El cliente nunca escribe archivos.
- Registra en el log cada alta/edición/borrado con el nombre del admin.
- mod_version=0.3.0, PR a main, etiqueta v0.3.0.
```

**Pruebas de Ian:** crear un botón desde la pantalla y que funcione al instante; el JSON refleja el cambio; un jugador sin `zian.gui.edit` no ve el botón "Editar" y, aunque se le fuerce un paquete, el servidor lo rechaza.

**Traer a Claude:** la sección del código que valida `EditButtonPayload`, para revisar la seguridad antes de seguir.

---

## Paso 6 — Fase 3: LuckPerms, bloqueo y mensajes

**Objetivo:** acceso por rango con botón bloqueado y mensaje de acceso denegado. (Spec: sección 7.2.)

**Prompt para ChatGPT:**

```
PASO 6: Fase 3 de la especificación (sección 7.2).
- Permisos: permission, permissions_any, permissions_all y group (equivale al
  nodo group.<nombre>). Orden de proveedores: Bukkit hasPermission → PermissionAPI
  de NeoForge si existe → op nivel fallback_op_level.
- Estados ENABLED / LOCKED / HIDDEN (show_when_denied, por defecto locked): icono
  atenuado con candado y tooltip. Al pulsar un botón bloqueado, el servidor envía
  el mensaje de denegado por chat y FeedbackPayload, con sonido opcional.
- config/zian_gui/settings.json con default_denied_message, default_denied_tooltip,
  denied_sound, show_permission_in_message (false) y fallback_op_level; denied_message
  por botón; textos traducibles en lang/es_es.json y en_us.json.
- Revalidar al abrir y al hacer clic; refresco opcional del estado mientras la
  pantalla está abierta.
- Cooldowns por botón y jugador, limpiados al desconectarse. Aviso informativo en
  log para botones as=console/player_op sin permission.
- Comando /ZianGui check <jugador> <menu> <boton> que muestre nodos evaluados, resultado,
  estado final y proveedor de permisos que respondió.
- mod_version=0.4.0, PR a main, etiqueta v0.4.0.
```

**Pruebas de Ian (LuckPerms):** permiso heredado de otro rango; comodín; permiso negado explícitamente; rango temporal que caduca; permiso quitado con la pantalla abierta; servidor sin LuckPerms (respaldo por op). Usar `/ZianGui check` para confirmar por qué sale bloqueado y apuntar si `group.<nombre>` responde.

**Traer a Claude:** la salida de `/ZianGui check` si algo no coincide con lo esperado.

---

## Paso 7 — Fase 4: API e integración con otros mods Zian

**Prompt para ChatGPT:**

```
PASO 7: Fase 4 de la especificación.
- ZianGuiApi pública: registrar acciones propias (por ejemplo open_gts) y menús
  desde código de otros mods, sin dependencia dura.
- Integración de ejemplo con Zian GTS y Zian Utilities solo mediante botones que
  ejecutan sus comandos (no modifiques esos repos sin avisarme).
- Documenta la API en el README con un ejemplo mínimo.
- mod_version=0.5.0, PR a main, etiqueta v0.5.0.
```

**Pruebas de Ian:** un botón abre el GTS o una pantalla de Utilities; si un mod externo registra una acción, el botón la usa.

---

## Paso 8 — Fase 5 y versión 1.0.0

**Prompt para ChatGPT:**

```
PASO 8: Fase 5 y estabilización.
- Listas dinámicas (homes/warps del jugador), paginación y confirmación opcional
  para botones peligrosos.
- Repaso final: README completo (requisitos, instalación en cliente y servidor,
  ejemplos de menús, comandos, permisos, cómo descargar builds), limpieza de
  warnings, revisión de seguridad de todos los payloads.
- mod_version=1.0.0, PR a main, etiqueta v1.0.0.
```

**Pruebas de Ian:** recorrido completo con jugadores de varios rangos y los casos de las fases anteriores.

---

## Cuando algo falla

- **CI en rojo:** pegar a ChatGPT el log del paso que falló y pedirle que corrija en `dev`; si se repite, traer el log a Claude.
- **ChatGPT improvisa fuera del paso:** recordarle la regla 5 del prompt de arranque.
- **Duda de diseño** (permisos, red, formato JSON): consultarla a Claude antes de que ChatGPT la resuelva por su cuenta, y si cambia una decisión, actualizar `docs/ESPECIFICACION_ZIAN_GUI.md`.
