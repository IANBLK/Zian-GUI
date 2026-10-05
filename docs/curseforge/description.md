**🎛️ ZIAN GUI — YOUR SERVER, ONE MENU**

Bring your server’s commands together in a compact, customizable in-game menu. Zian GUI lets players open useful features with a click while administrators control each button’s name, item icon, command, permissions, and order.

Designed for Minecraft 1.21.1 with NeoForge and tested on Youer 1.21.1, the interface uses a charcoal background, gold borders, compact buttons, and automatic pagination.

**⌨️ OPEN YOUR MENU WITH Z**

Press **Z** to open the main menu, or use **/ZianGui**. The shortcut can be changed under **Options → Controls → Zian GUI → Open Zian GUI**. Existing installations may retain an older key assignment, so check your Controls settings after updating.

**🛠️ EDIT BUTTONS INSIDE THE GAME**

Administrators can open the editor with **/ZianGui edit**, or click **Edit** in the menu when they have the required permission.

Create new buttons, rename existing ones, change their commands and permission requirements, reorder them, or remove features your server does not use.

Hold an item in your **main hand** and select the hand icon option to use that item type as the button’s icon. The server reads the held item when you save and does not consume it. Custom names, enchantments, and item components are not copied into the icon.

Positions start at **1**, following the order left, right, then the next row. Menus support up to **24 buttons**, with pages when needed. Changes are saved on the server immediately, without a restart or a manual reload.

**🔗 SHORTCUTS FOR YOUR SERVER FEATURES**

The default menu includes **Spawn, Heal Pokémon, PC, Ender Chest, Fly, Gachas, Quests, GTS, and Medals**.

These are shortcuts to commands supplied by **Cobblemon, EternalCore, Zian Utilities, Zian GTS, and Zian RCT**. Zian GUI does not provide those features itself. Install the relevant mod or plugin, configure its command, and grant its permissions before using the button.

The menu editor lets you remove unavailable integrations or replace them with your own server commands. Those optional integrations are not required for Zian GUI itself to load.

**📜 COMMANDS**

**/ZianGui** — Open the main menu.

**/ZianGui open** — Open the main menu.

**/ZianGui open <menu>** — Open another menu configured on the server.

**/ZianGui edit** — Open the main menu’s button editor.

**/ZianGui edit <menu>** — Edit buttons in another existing menu.

**/ZianGui reload** — Reload the server’s menu files. Invalid files leave the previous working configuration active; a successful reload closes existing menu and editor sessions.

**🔐 LUCKPERMS AND PLAYER PERMISSIONS**

On Youer, Zian GUI checks the player’s Bukkit permissions, allowing administrators to manage access through **LuckPerms groups**.

**zian.gui.open** — Allows opening Zian GUI.

**zian.gui.menu.principal** — Allows opening the default main menu. Other menus use the permission configured in their own file.

**zian.gui.edit** — Administrative access to the in-game button editor.

**zian.gui.reload** — Administrative access to reload menu files.

Each button can require additional permission nodes, and its destination command still checks the player’s permissions. Adding a button does not automatically grant access to that command. Keep editor and reload permissions in an administrative group.

Commands run **as the player**, without temporary OP or console execution. Explicit permission denials are respected, including for operators. On standard NeoForge without Bukkit, protected access currently uses the level-2 operator fallback; a native NeoForge permission provider is not included.

The full default-group permission list is available in the [permission guide](https://github.com/IANBLK/Zian-GUI/blob/dev/docs/permissions-default.txt).

**💾 SERVER CONFIGURATION AND BACKUPS**

Menu files are stored in **config/zian_gui/menus/**. Existing menu files are preserved when updating the mod.

The editor validates changes before saving and keeps a **.json.bak** copy of the previous menu file. Outdated or conflicting edits are rejected to prevent overwriting another administrator’s changes. Successful edits close old sessions and refresh the editor.

Multiple menus can be defined in JSON. The in-game editor currently manages buttons in existing menus; creating menus or changing an entire menu’s title and access permission is done through its JSON file.

Read the [in-game editor guide](https://github.com/IANBLK/Zian-GUI/blob/dev/docs/EDITOR.md) for examples and recovery instructions.

**📦 INSTALLATION**

Use **Minecraft 1.21.1**, **Java 21**, and **NeoForge 21.1.228 or a compatible later 21.1 build**.

Install the **same Zian GUI version on both the client and server**, then restart both. Beta 7 uses network protocol 3 and must not be mixed with clients from earlier betas.

For Youer servers, install LuckPerms as a plugin if you want group-based access management. Install the optional mods or plugins that supply your chosen button commands. Mod interfaces may also require their corresponding mod on the client.

**🚧 BETA — ACTIVE DEVELOPMENT**

Zian GUI is in active development. Features, configuration options, and visual details may evolve during the beta period.

When reporting an issue, include your Zian GUI version, Minecraft and server versions, the affected button or command, and relevant logs. Report problems through [GitHub Issues](https://github.com/IANBLK/Zian-GUI/issues).

**Created by IANBLK • Minecraft 1.21.1 • NeoForge • Youer • MIT License**
