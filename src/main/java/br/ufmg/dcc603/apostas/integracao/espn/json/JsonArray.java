package br.ufmg.dcc603.apostas.integracao.espn.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class JsonArray extends JsonElement implements Iterable<JsonElement> {
    private final List<JsonElement> elements = new ArrayList<>();

    public void add(JsonElement element) {
        elements.add(element == null ? JsonPrimitive.NULL : element);
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public JsonElement get(int index) {
        return elements.get(index);
    }

    public JsonObject getObject(int index) {
        JsonElement el = get(index);
        return el != null && el.isObject() ? el.asObject() : null;
    }

    public String getString(int index) {
        JsonElement el = get(index);
        return el != null ? el.asString() : null;
    }

    public List<JsonElement> asList() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public Iterator<JsonElement> iterator() {
        return elements.iterator();
    }

    @Override
    public String toString() {
        return elements.toString();
    }
}
