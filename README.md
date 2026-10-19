# Zian GUI 0.1.1

**Language notice:** The current in-game interface, preset dialogues and administrator messages are primarily Spanish. English localization is incomplete. The English guide explains the controls and includes their Spanish button names. Changing Minecraft to English does not translate administrator-written NPC names, dialogues or configuration text.

[English guide](docs/GUIDE.md) · [Guía en español](docs/GUIA_ES.md) · [Updating and recovery](docs/UPDATING.md) · [Current changes](CHANGELOG.md) · [Download](https://github.com/IANBLK/Zian-GUI/releases/latest)

Bring server commands together in a compact, configurable native menu.

Open the main menu with the configurable Z shortcut or /ZianGui. Administrators use /ZianGui edit to add, rename, reorder or remove buttons and configure item icons, commands and permissions.

Responsive layouts and pagination adapt the menu to different GUI scales. Actions run through server-side permission checks while preserving the player's identity.

Default shortcuts can open installed Zian features or commands from other mods/plugins. Zian GUI does not supply those features itself: configure or remove unavailable shortcuts.

Install on client and server with Minecraft 1.21.1, Java 21 and NeoForge 21.1.x. Other Zian mods and plugins are optional integrations. Existing key assignments can persist after updates; check Controls if needed.

## Español

Reúna comandos en un menú nativo con botones, iconos y permisos configurables. Abra con Z o /ZianGui y edite con /ZianGui edit. Incluye paginación y adaptación a la escala. Puede cambiar o quitar accesos a mods o plugins.

## Latest update — 0.1.1

Buttons now include their borders in the clickable area, use a 150 ms action delay and protect actions with one-use sessions. The editor explains requirements, generates identifiers automatically and preserves navigation. Added /zgui. Update both sides: network protocol 4.

## Novedades — Español

Los bordes de los botones ya responden al clic, con espera de 150 ms y sesiones de un solo uso. El editor explica requisitos y genera identificadores. Conserva página y navegación; nuevo alias /zgui. Actualiza cliente y servidor juntos.

Minecraft 1.21.1 · NeoForge · Java 21. Install the same mod version on client and server. Back up your world before upgrading; replace the old JAR instead of keeping both copies.
