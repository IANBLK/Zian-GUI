**✨ ZIAN GUI — 0.1.0-beta.7**

**🛠️ NEW IN-GAME BUTTON EDITOR**

Added an administrator editor accessible through the **Edit** button or **/ZianGui edit [menu]**.

Create, rename, reorder, and delete buttons. Change their commands and permission requirements, and select an item held in the main hand as the icon without consuming it.

**💾 SAVE AND RECOVER YOUR MENUS**

Editor changes save immediately to the original server menu file, with an atomic replacement and a **.json.bak** backup of the previous contents.

Invalid changes, outdated editor sessions, and conflicting file edits are rejected. Successful saves close old menu sessions and refresh the administrator’s editor.

Empty menus are supported, allowing administrators to remove every default button and add their own.

**🔐 ADMINISTRATIVE ACCESS**

Added **zian.gui.edit** for editor access. The server checks this permission on every save.

Existing player permissions and destination-command permission checks remain in place. Button actions continue to run as the player.

**📦 UPDATE REQUIREMENTS**

Install **0.1.0-beta.7 on both client and server**. This release uses network protocol 3; earlier beta clients are incompatible.

Existing JSON menus are preserved. The **Z shortcut**, compact menu layout, and Youer command-routing fixes remain available.

**✅ VALIDATION**

The build passed **37 automated tests**, covering existing behavior, editor changes, ordering, validation, backups, conflicting edits, and packet limits.

**This is a beta release.**
