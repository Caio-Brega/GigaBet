package br.ufmg.dcc603.apostas.integracao.espn.json;

public abstract class JsonElement {
    public boolean isObject() {
        return this instanceof JsonObject;
    }

    public boolean isArray() {
        return this instanceof JsonArray;
    }

    public boolean isPrimitive() {
        return this instanceof JsonPrimitive;
    }

    public boolean isNull() {
        return this instanceof JsonPrimitive p && p.isNull();
    }

    public JsonObject asObject() {
        if (isObject()) {
            return (JsonObject) this;
        }
        throw new IllegalStateException("Nao e JsonObject: " + this);
    }

    public JsonArray asArray() {
        if (isArray()) {
            return (JsonArray) this;
        }
        throw new IllegalStateException("Nao e JsonArray: " + this);
    }

    public JsonPrimitive asPrimitive() {
        if (isPrimitive()) {
            return (JsonPrimitive) this;
        }
        throw new IllegalStateException("Nao e JsonPrimitive: " + this);
    }

    public String asString() {
        return asPrimitive().asString();
    }

    public int asInt() {
        return asPrimitive().asInt();
    }

    public long asLong() {
        return asPrimitive().asLong();
    }

    public double asDouble() {
        return asPrimitive().asDouble();
    }

    public boolean asBoolean() {
        return asPrimitive().asBoolean();
    }
}
