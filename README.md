# Zian GUI

Menús configurables con pantalla propia para Minecraft 1.21.1, NeoForge y Youer.
Licencia MIT. Autor: IANBLK.

## Estado: 0.0.1 — base

Esta versión solo registra el mod y escribe `[ZianGUI] cargado` en el log.
Todavía no tiene pantallas, comandos, JSON, permisos ni editor.

## Requisitos e instalación

- Java 21 y Minecraft 1.21.1.
- NeoForge 21.1.228 o posterior de la rama 21.1 (base alineada con Zian GTS).
- Para probar la integración futura: Youer 1.21.1.
- Instalar el mismo JAR en `mods/` del cliente y del servidor.
- LuckPerms será opcional; no es una dependencia de esta base.
- No requiere Cobblemon, Zian GTS, Zian RCT ni Zian Utilities.

Arranca cliente y servidor y comprueba `[ZianGUI] cargado` en ambos logs.
La carga en Youer necesita una prueba real de Ian; el CI no la certifica.

## Descargar builds

[Actions](https://github.com/IANBLK/Zian-GUI/actions): abre una ejecución verde,
descarga el artifact `zian-gui-<commit>` y extrae `zian-gui-0.0.1.jar`.
Los artifacts duran 14 días y requieren iniciar sesión en GitHub.
[Releases](https://github.com/IANBLK/Zian-GUI/releases) publica el JAR al crear
una etiqueta `v*`; las etiquetas con guion son versiones preliminares.

## Compilar

Linux: `./gradlew build --no-daemon`. Windows: `gradlew.bat build --no-daemon`.
Se incluyen Gradle Wrapper 8.11 y ModDevGradle 2.0.107, como en Zian GTS.
El JAR jugable queda en `build/libs/`.

## Desarrollo

Trabajar en `dev`. Integrar a `main` por PR cuando esté compilado y probado.
La especificación está en [docs/ESPECIFICACION_ZIAN_GUI.md](docs/ESPECIFICACION_ZIAN_GUI.md).
La revisión de interfaz está en [docs/ANALISIS_ZIANRCT_UI.md](docs/ANALISIS_ZIANRCT_UI.md).

Comandos, permisos y ejemplos de menús se añadirán al implementar sus fases.
La instalación obligatoria en ambos lados se comprobará mediante el protocolo
requerido de red desde la fase de networking; `side="BOTH"` en las dependencias
por sí solo no impide que un cliente sin este mod se conecte a esta base.
