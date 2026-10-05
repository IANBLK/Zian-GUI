package com.ianblk.ziangui.server;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Server-owned configuration. Only validated snapshots become visible to players. */
public final class MenuConfig {
    public static final int MAX_BUTTONS = 24;
    public record Action(String id, String label, String icon, List<String> permissions, String command) {
        public Action { permissions = List.copyOf(permissions); }
        public boolean allowed(Predicate<String> check) { return permissions.stream().allMatch(check); }
    }
    public record Menu(String id, String title, String permission, List<Action> buttons) {
        public Menu { buttons = List.copyOf(buttons); }
    }
    private Map<String, Menu> menus = Map.of();
    private Map<String, Path> sources = Map.of();
    public Map<String, Menu> menus() { return menus; }
    public Menu get(String id) { return menus.get(id); }

    public void reload(Path directory) throws IOException {
        Files.createDirectories(directory);
        if (Files.isSymbolicLink(directory)) throw new IOException("Directorio de menús no regular");
        List<Path> files;
        try (var stream = Files.list(directory)) {
            files = stream.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().limit(17).toList();
        }
        if (files.isEmpty()) {
            try (var in = MenuConfig.class.getResourceAsStream("/defaults/principal.json")) {
                if (in == null) throw new IOException("Falta el menú de ejemplo en el JAR");
                Files.copy(in, directory.resolve("principal.json")); // Never overwrite an existing menu.
            }
            files = List.of(directory.resolve("principal.json"));
        }
        if (files.size() > 16) throw new IOException("Máximo 16 menús");
        Map<String, Menu> next = new LinkedHashMap<>();
        Map<String, Path> paths = new LinkedHashMap<>();
        for (Path file : files) {
            try {
                if (Files.isSymbolicLink(file) || !Files.isRegularFile(file)) throw new IllegalArgumentException("Archivo no regular");
                if (Files.size(file) > 65_536) throw new IllegalArgumentException("Máximo 64 KiB por menú");
                Menu menu = parse(Files.readString(file, StandardCharsets.UTF_8));
                if (next.putIfAbsent(menu.id(), menu) != null) throw new IllegalArgumentException("ID de menú duplicado");
                paths.put(menu.id(), file);
            } catch (RuntimeException error) {
                throw new IOException(file.getFileName() + ": " + error.getMessage(), error);
            }
        }
        if (!next.containsKey("principal")) throw new IOException("Debe existir un menú con id principal");
        menus = Collections.unmodifiableMap(next); // Atomic logical swap after ALL files validate.
        sources = Map.copyOf(paths);
    }
    public static String json(Menu menu) {
        JsonObject root = new JsonObject();
        root.addProperty("id", menu.id()); root.addProperty("title", menu.title()); root.addProperty("permission", menu.permission());
        JsonArray buttons = new JsonArray();
        for (Action a : menu.buttons()) {
            JsonObject b = new JsonObject();
            b.addProperty("id", a.id()); b.addProperty("name", a.label()); b.addProperty("icon", a.icon());
            JsonArray nodes = new JsonArray(); a.permissions().forEach(nodes::add);
            b.add("permissions", nodes); b.addProperty("command", a.command()); buttons.add(b);
        }
        root.add("buttons", buttons);
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root);
    }
    /** Revision covers all menus, including edits made outside the game. */
    public String revision() {
        try {
            var hash = java.security.MessageDigest.getInstance("SHA-256");
            new TreeMap<>(menus).values().forEach(m -> hash.update(json(m).getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(hash.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public void save(Path directory, String expectedRevision, Menu replacement) throws IOException {
        Menu validated = parse(json(replacement));
        for (Path source : sources.values()) {
            if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Un archivo del menú cambió o desapareció; vuelve a cargar los menús.");
        }
        MenuConfig fresh = new MenuConfig(); fresh.reload(directory);
        if (!fresh.revision().equals(expectedRevision) || !fresh.sources.equals(sources)) throw new IOException("El menú cambió. Cierra y vuelve a abrir el editor (recarga si editaste archivos).");
        Path target = fresh.sources.get(validated.id());
        if (target == null) throw new IOException("Menú inexistente");
        byte[] bytes = json(validated).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 65_536) throw new IOException("Máximo 64 KiB por menú");
        Path backup = target.resolveSibling(target.getFileName() + ".bak");
        if (Files.exists(backup, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(backup, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Respaldo no regular");
        Path temp = Files.createTempFile(directory, ".edit-", ".tmp");
        Path backupTemp = null;
        try {
            backupTemp = Files.createTempFile(directory, ".backup-", ".tmp");
            Files.copy(target, backupTemp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(backupTemp, backup, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (var channel = java.nio.channels.FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            Map<String, Menu> next = new LinkedHashMap<>(fresh.menus); next.put(validated.id(), validated);
            menus = Collections.unmodifiableMap(next); sources = fresh.sources;
        } finally { Files.deleteIfExists(temp); if (backupTemp != null) Files.deleteIfExists(backupTemp); }
    }
    public static Menu parse(String json) {
        validateJson(json);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        fields(root, Set.of("id", "title", "permission", "buttons"));
        String id = identifier(string(root, "id", 32));
        String title = string(root, "title", 64);
        String permission = permission(string(root, "permission", 128));
        if (title.isBlank()) throw new IllegalArgumentException("Título vacío");
        JsonArray buttons = root.getAsJsonArray("buttons");
        if (buttons == null || buttons.size() > MAX_BUTTONS)
            throw new IllegalArgumentException("Máximo 24 botones");
        List<Action> actions = new ArrayList<>(); Set<String> ids = new HashSet<>();
        for (JsonElement element : buttons) {
            JsonObject b = element.getAsJsonObject();
            fields(b, Set.of("id", "name", "icon", "permissions", "command"));
            String buttonId = identifier(string(b, "id", 32));
            if (!ids.add(buttonId)) throw new IllegalArgumentException("Botón duplicado: " + buttonId);
            String label = string(b, "name", 64), icon = string(b, "icon", 128);
            if (label.isBlank() || !icon.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                throw new IllegalArgumentException("Nombre o icono inválido: " + buttonId);
            String command = string(b, "command", 256).trim();
            if (!command.matches("[A-Za-z0-9_:.-]+(?: [^\\p{Cntrl};]+)?"))
                throw new IllegalArgumentException("Comando inválido (sin /, saltos de línea ni ;): " + buttonId);
            JsonArray nodes = b.getAsJsonArray("permissions");
            if (nodes == null || nodes.size() > 8) throw new IllegalArgumentException("Máximo 8 permisos por botón");
            List<String> permissions = new ArrayList<>();
            for (var node : nodes) {
                if (!node.isJsonPrimitive() || !node.getAsJsonPrimitive().isString())
                    throw new IllegalArgumentException("Permiso debe ser texto");
                String value = node.getAsString();
                if (value.length() > 128) throw new IllegalArgumentException("Permiso demasiado largo");
                permissions.add(permission(value));
            }
            actions.add(new Action(buttonId, label, icon, permissions, command));
        }
        return new Menu(id, title, permission, actions);
    }
    private static void validateJson(String json) {
        try (var reader = new com.google.gson.stream.JsonReader(new java.io.StringReader(json))) {
            reader.setLenient(false);
            inspect(reader, 0);
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT)
                throw new IllegalArgumentException("Contenido después del JSON");
        } catch (IOException error) { throw new IllegalArgumentException("JSON inválido: " + error.getMessage(), error); }
    }
    private static void inspect(com.google.gson.stream.JsonReader reader, int depth) throws IOException {
        if (depth > 8) throw new IllegalArgumentException("JSON demasiado profundo");
        switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject(); Set<String> keys = new HashSet<>();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (!keys.add(key)) throw new IllegalArgumentException("Campo duplicado: " + key);
                    inspect(reader, depth + 1);
                }
                reader.endObject();
            }
            case BEGIN_ARRAY -> {
                reader.beginArray();
                while (reader.hasNext()) inspect(reader, depth + 1);
                reader.endArray();
            }
            case STRING, NUMBER -> reader.nextString();
            case BOOLEAN -> reader.nextBoolean();
            case NULL -> reader.nextNull();
            default -> throw new IllegalArgumentException("Valor JSON inválido");
        }
    }
    private static void fields(JsonObject object, Set<String> allowed) {
        for (String field : object.keySet()) if (!allowed.contains(field))
            throw new IllegalArgumentException("Campo no soportado: " + field);
    }
    private static String string(JsonObject object, String key, int max) {
        var value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Falta texto: " + key);
        String s = value.getAsString();
        if (s.length() > max || s.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Texto inválido: " + key);
        return s;
    }
    private static String identifier(String s) {
        if (!s.matches("[a-z0-9_]{1,32}")) throw new IllegalArgumentException("ID inválido: " + s);
        return s;
    }
    private static String permission(String s) {
        if (!s.matches("[a-zA-Z0-9_.-]{1,128}")) throw new IllegalArgumentException("Permiso inválido: " + s);
        return s;
    }
}
