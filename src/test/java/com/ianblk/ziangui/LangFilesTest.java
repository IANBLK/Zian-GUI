package com.ianblk.ziangui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LangFilesTest {
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?[sd]");

    private static Map<String, String> load(String name) throws Exception {
        var json = JsonParser.parseString(new String(
            LangFilesTest.class.getResourceAsStream("/assets/ziangui/lang/" + name).readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        var map = new TreeMap<String, String>();
        json.entrySet().forEach(e -> map.put(e.getKey(), e.getValue().getAsString()));
        return map;
    }
    private static String placeholders(String text) {
        var found = new java.util.ArrayList<String>();
        var m = PLACEHOLDER.matcher(text);
        while (m.find()) found.add(m.group());
        java.util.Collections.sort(found);
        return String.join(",", found);
    }

    @Test void allLanguagesHaveSameKeysAndPlaceholders() throws Exception {
        var es = load("es_es.json");
        for (var other : new String[] {"en_us.json", "es_mx.json"}) {
            var map = load(other);
            assertEquals(es.keySet(), map.keySet(), "Claves distintas en " + other);
            es.forEach((key, text) -> assertEquals(placeholders(text), placeholders(map.get(key)), key + " en " + other));
        }
    }
}
