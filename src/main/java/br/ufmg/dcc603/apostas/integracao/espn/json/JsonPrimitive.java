package br.ufmg.dcc603.apostas.integracao.espn.json;

public final class JsonPrimitive extends JsonElement {
    public static final JsonPrimitive NULL = new JsonPrimitive(null);
    public static final JsonPrimitive TRUE = new JsonPrimitive(Boolean.TRUE);
    public static final JsonPrimitive FALSE = new JsonPrimitive(Boolean.FALSE);

    private final Object value;

    public JsonPrimitive(Object value) {
        this.value = value;
    }

    @Override
    public boolean isNull() {
        return value == null;
    }

    @Override
    public String asString() {
        return value == null ? null : value.toString();
    }

    @Override
    public int asInt() {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return (int) Double.parseDouble(s.trim());
            }
        }
        return 0;
    }

    @Override
    public long asLong() {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return (long) Double.parseDouble(s.trim());
            }
        }
        return 0L;
    }

    @Override
    public double asDouble() {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            return Double.parseDouble(s.trim());
        }
        return 0.0;
    }

    @Override
    public boolean asBoolean() {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            return Boolean.parseBoolean(s.trim());
        }
        return false;
    }

    public Object rawValue() {
        return value;
    }

    @Override
    public String toString() {
        return value == null ? "null" : value.toString();
    }
}
