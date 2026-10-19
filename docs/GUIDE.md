# Zian GUI 0.1.1 — English guide

**Language notice:** The current in-game interface, preset dialogues and administrator messages are primarily Spanish. English localization is incomplete. The English guide explains the controls and includes their Spanish button names. Changing Minecraft to English does not translate administrator-written NPC names, dialogues or configuration text.

## Players: open the menu

Press **Z**, `/ZianGui` or `/zgui`. Rebind the key in Minecraft Controls if another mod uses it. Choose a button or a navigation arrow. Zian GUI launches the configured command as your player; it does not provide the target feature, grant OP or bypass that command's permissions. Remove/configure shortcuts for unavailable mods/plugins.

## Administrators: configure buttons

1. Hold the desired icon item in your main hand. Run `/ZianGui edit` or choose **Editar** (Edit). `/ZianGui edit <menu-id>` edits another existing menu.
2. Choose **Añadir** (Add) or select a button. Set displayed name, command, additional permission nodes and requirement help. Leave a new identifier empty to generate it. IDs use lowercase letters, numbers and underscores.
3. Choose the held icon or preserve its current icon. The item is not consumed; only its type is copied, not its components/enchantments. Set its position and choose **Guardar** (Save). Position changes shift other buttons. **Eliminar** (Delete) needs confirmation. Up to 24 buttons per menu.
4. Editor access needs `zian.gui.edit`; reload needs `zian.gui.reload`. On standalone NeoForge administration falls back to OP level 2. Explicit LuckPerms/Bukkit denials remain effective. Do not grant editing to normal players.

## Menu files and recovery

Menus live in `config/zian_gui/menus/`. The initial `principal.json` is generated only if needed. Existing files are retained. In-game editing changes buttons, not whole-menu titles/permissions or new menu files. To add a menu, copy a valid file and change its ID/title.

```json
{"id":"principal","title":"Server menu","permission":"zian.gui.menu.principal","buttons":[{"id":"spawn","name":"Spawn","icon":"minecraft:compass","permissions":[],"command":"spawn"}]}
```

The example assumes `/spawn` exists on the server. `permissions` adds requirements; it grants none. Access also checks `zian.gui.open` and the configured menu permission. See the shipped example in `src/main/resources/defaults/principal.json`.

Saving atomically replaces the original file after making its `.json.bak` copy. Concurrent edits are rejected. To restore, stop editing, restore the matching JSON and run `/ZianGui reload`. Current protocol **4** needs matching client/server versions.

## Installation, updating and recovery

Use Minecraft **1.21.1**, **Java 21** and **NeoForge**, with the separate required libraries listed in the README. Stop the server and back up the entire world, configuration and mod list before replacing a version. Install the same current JAR on the server and every client. Remove the previous JAR, keep the required libraries on both sides, start and inspect the log, then verify your existing configuration and progress. For rollback, stop and restore the complete matching backup and mod versions together. Do not edit schema numbers to force migration.

Youer has been tested; this does not guarantee every hybrid server/plugin combination. Command-whitelist plugins may separately block public commands. Allow the needed public roots while keeping administrative operations restricted. LuckPerms is optional; administrator configuration remains protected.

Keep world data and configuration together. Optional SQLite maintenance is explicit, not required automatically by an update. Never edit a live database or delete a journal to clear an error. For an uncertain payment/delivery, keep its reference, reconcile the recorded outcome with inventory/PC/wallet state and use the administrator review flow. Confirmation records a verified result; it must never be used to guess or generate a second reward.

Report the mod/library versions, reproduction steps and sanitized logs through this repository's Issues. Do not upload credentials, tokens, player databases or private world backups.
