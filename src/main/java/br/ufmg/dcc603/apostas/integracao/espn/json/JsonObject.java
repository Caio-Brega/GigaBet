package br.ufmg.dcc603.apostas.integracao.espn.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JsonObject extends JsonElement {
    private final Map<String, JsonElement> members = new LinkedHashMap<>();

    public void add(String key, JsonElement value) {
        members.put(key, value == null ? JsonPrimitive.NULL : value);
    }

    public void addProperty(String key, String value) {
        add(key, new JsonPrimitive(value));
    }

    public void addProperty(String key, Number value) {
        add(key, new JsonPrimitive(value));
    }

    public void addProperty(String key, Boolean value) {
        add(key, new JsonPrimitive(value));
    }

    public boolean has(String key) {
        return members.containsKey(key);
    }

    public JsonElement get(String key) {
        return members.get(key);
    }

    public JsonObject getObject(String key) {
        JsonElement el = get(key);
        return el != null && el.isObject() ? el.asObject() : null;
    }

    public JsonArray getArray(String key) {
        JsonElement el = get(key);
        return el != null && el.isArray() ? el.asArray() : null;
    }

    public String getString(String key, String defaultValue) {
        JsonElement el = get(key);
        if (el == null || el.isNull()) {
            return defaultValue;
        }
        return el.asString();
    }

    public int getInt(String key, int defaultValue) {
        JsonElement el = get(key);
        if (el == null || el.isNull()) {
            return defaultValue;
        }
        try {
            return el.asInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        JsonElement el = get(key);
        if (el == null || el.isNull()) {
            return defaultValue;
        }
        try {
            return el.asDouble();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        JsonElement el = get(key);
        if (el == null || el.isNull()) {
            return defaultValue;
        }
        try {
            return el.asBoolean();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public Set<String> keySet() {
        return Collections.unmodifiableSet(members.keySet());
    }

    public Map<String, JsonElement> asMap() {
        return Collections.unmodifiableMap(members);
    }

    @Override
    public String toString() {
        return members.toString();
    }
}
