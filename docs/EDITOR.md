# Editor de botones dentro del juego — Beta 7

Instala `zian-gui-0.1.0-beta.7.jar` en **cliente y servidor**. El protocolo ahora es 3:
no mezcles esta versión con clientes de Betas anteriores. Los menús JSON existentes
se conservan y no necesitan conversión. La tecla Z sigue abriendo el menú principal.

## Permiso administrativo

Usa un grupo separado; no concedas este permiso a todos los jugadores.
Ejemplo desde consola (puedes usar tu grupo administrativo existente):

```text
lp creategroup ziangui-admin
lp group ziangui-admin permission set zian.gui.edit true
lp user IANBLK parent add ziangui-admin
```

El editor puede cambiar qué comandos se ofrecen y qué permisos adicionales pide
cada botón. Sigue ejecutando las acciones como jugador, respetando los permisos
del comando de destino. No ejecuta como consola ni concede OP.
En Youer se consulta LuckPerms/Bukkit y una denegación explícita también se respeta
para operadores. En NeoForge sin Bukkit, el acceso administrativo requiere OP nivel 2.

## Uso

1. Sostén el objeto que quieras usar como icono en la **mano principal**.
2. Pulsa Z y después **Editar**, o usa `/ZianGui edit`.
   Para otro menú existente: `/ZianGui edit <id>`.
3. Pulsa **Añadir**, o selecciona un botón existente para modificarlo.
4. Completa ID (minúsculas, números o `_`), nombre, comando y permisos.
   Puedes escribir el comando con o sin `/`. Los permisos van separados por comas;
   si quedan vacíos, solo se exigen los permisos del menú y del comando de destino.
5. La posición empieza en 1 y sigue el orden izquierda, derecha, siguiente fila.
   Cambiarla mueve el botón y desplaza los siguientes; no crea espacios vacíos.
6. Selecciona **Icono: objeto en la mano** para cambiar el icono. Para editar solo
   el nombre o el comando, conserva **Icono: conservar actual**.
7. Pulsa **Guardar**. El servidor toma el tipo de objeto realmente sostenido
   en ese momento; no lo consume. No copia encantamientos, nombres ni componentes.
   No requiere `/ZianGui reload` ni reiniciar.

Para quitar una integración ausente, selecciona su botón y pulsa **Eliminar** dos veces
(la segunda confirma la operación). Puedes quitar todos los botones y añadir otros
después desde `/ZianGui edit`. El editor admite hasta 24 botones por menú y paginación.
Los jugadores necesitan los permisos que escribas y los del comando de destino.
Crear un botón no concede permisos de LuckPerms automáticamente.

## Guardado y recuperación

El editor modifica el archivo JSON original del menú, aunque su nombre no coincida
con el ID. Antes de reemplazarlo crea una copia del contenido previo con extensión
`.json.bak`. Cada guardado actualiza ese respaldo. El reemplazo es atómico;
si el sistema de archivos no lo admite, la operación se rechaza.

Una edición correcta cierra los menús y editores antiguos para impedir clics sobre
botones que cambiaron. El administrador recibe el menú actualizado en el editor.
Datos inválidos conservan el menú vigente. Una edición concurrente o un cambio
externo de los archivos se rechaza: recarga los archivos si corresponde y vuelve
a abrir el editor antes de intentar guardar.

Para restaurar, copia el contenido del `.json.bak` al JSON correspondiente y ejecuta
`/ZianGui reload` desde consola o con el permiso administrativo `zian.gui.reload`.
El respaldo no se carga como otro menú. Cierra el editor al terminar; las sesiones
caducan en cinco minutos y el servidor vuelve a comprobar el permiso en cada guardado.

El editor modifica botones de menús existentes. La creación de nuevos menús y los
cambios de título o permiso del menú completo siguen realizándose en sus archivos JSON.

## Prueba recomendada

- Añadir un botón con un icono en la mano y comprobar que aparece con el nombre elegido.
- Editar su nombre, comando, permisos e icono; moverlo a la primera posición.
- Eliminar un botón de una integración que no está instalada.
- Salir, volver a abrir y reiniciar el servidor: comprobar que conserva los cambios.
- Sin `zian.gui.edit`, comprobar que no aparece Editar ni permite `/ZianGui edit`.
- Comprobar con otra cuenta que los permisos de los botones siguen aplicándose.
- Dos administradores editando: un guardado debe invalidar el editor del otro.
