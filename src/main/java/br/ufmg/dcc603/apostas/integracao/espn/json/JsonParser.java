package br.ufmg.dcc603.apostas.integracao.espn.json;

public final class JsonParser {
    private final String src;
    private final int len;
    private int pos;

    private JsonParser(String src) {
        this.src = src != null ? src : "";
        this.len = this.src.length();
        this.pos = 0;
    }

    public static JsonElement parse(String json) {
        if (json == null || json.isBlank()) {
            return JsonPrimitive.NULL;
        }
        JsonParser parser = new JsonParser(json);
        JsonElement element = parser.parseValue();
        parser.skipWhitespace();
        return element;
    }

    public static JsonObject parseObject(String json) {
        JsonElement el = parse(json);
        if (el instanceof JsonObject obj) {
            return obj;
        }
        throw new IllegalArgumentException("JSON nao e um objeto: " + json);
    }

    public static JsonArray parseArray(String json) {
        JsonElement el = parse(json);
        if (el instanceof JsonArray arr) {
            return arr;
        }
        throw new IllegalArgumentException("JSON nao e um array: " + json);
    }

    private JsonElement parseValue() {
        skipWhitespace();
        if (pos >= len) {
            return JsonPrimitive.NULL;
        }
        char c = src.charAt(pos);
        if (c == '{') {
            return parseObjectInternal();
        }
        if (c == '[') {
            return parseArrayInternal();
        }
        if (c == '"') {
            return new JsonPrimitive(parseString());
        }
        if (c == 't' || c == 'T') {
            consumeKeyword("true");
            return JsonPrimitive.TRUE;
        }
        if (c == 'f' || c == 'F') {
            consumeKeyword("false");
            return JsonPrimitive.FALSE;
        }
        if (c == 'n' || c == 'N') {
            consumeKeyword("null");
            return JsonPrimitive.NULL;
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return parseNumber();
        }
        throw new IllegalArgumentException("Caractere inesperado no JSON na posicao " + pos + ": '" + c + "'");
    }

    private JsonObject parseObjectInternal() {
        pos++; // skip '{'
        JsonObject obj = new JsonObject();
        skipWhitespace();
        if (pos < len && src.charAt(pos) == '}') {
            pos++;
            return obj;
        }

        while (pos < len) {
            skipWhitespace();
            if (pos >= len) break;
            if (src.charAt(pos) == '}') {
                pos++;
                return obj;
            }

            if (src.charAt(pos) != '"') {
                throw new IllegalArgumentException("Esperava chave entre aspas na posicao " + pos);
            }
            String key = parseString();
            skipWhitespace();
            if (pos >= len || src.charAt(pos) != ':') {
                throw new IllegalArgumentException("Esperava ':' apos chave '" + key + "' na posicao " + pos);
            }
            pos++; // skip ':'
            JsonElement val = parseValue();
            obj.add(key, val);

            skipWhitespace();
            if (pos < len && src.charAt(pos) == ',') {
                pos++;
                skipWhitespace();
            } else if (pos < len && src.charAt(pos) == '}') {
                pos++;
                return obj;
            } else {
                break;
            }
        }
        return obj;
    }

    private JsonArray parseArrayInternal() {
        pos++; // skip '['
        JsonArray arr = new JsonArray();
        skipWhitespace();
        if (pos < len && src.charAt(pos) == ']') {
            pos++;
            return arr;
        }

        while (pos < len) {
            skipWhitespace();
            if (pos >= len) break;
            if (src.charAt(pos) == ']') {
                pos++;
                return arr;
            }

            JsonElement val = parseValue();
            arr.add(val);

            skipWhitespace();
            if (pos < len && src.charAt(pos) == ',') {
                pos++;
                skipWhitespace();
            } else if (pos < len && src.charAt(pos) == ']') {
                pos++;
                return arr;
            } else {
                break;
            }
        }
        return arr;
    }

    private String parseString() {
        pos++; // skip opening '"'
        StringBuilder sb = new StringBuilder();
        while (pos < len) {
            char c = src.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (pos >= len) break;
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 <= len) {
                            String hex = src.substring(pos, pos + 4);
                            pos += 4;
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                            } catch (NumberFormatException e) {
                                sb.append("\\u").append(hex);
                            }
                        }
                    }
                    default -> sb.append(esc);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private JsonElement parseNumber() {
        int start = pos;
        if (pos < len && src.charAt(pos) == '-') {
            pos++;
        }
        while (pos < len && src.charAt(pos) >= '0' && src.charAt(pos) <= '9') {
            pos++;
        }
        boolean isFloat = false;
        if (pos < len && src.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            while (pos < len && src.charAt(pos) >= '0' && src.charAt(pos) <= '9') {
                pos++;
            }
        }
        if (pos < len && (src.charAt(pos) == 'e' || src.charAt(pos) == 'E')) {
            isFloat = true;
            pos++;
            if (pos < len && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                pos++;
            }
            while (pos < len && src.charAt(pos) >= '0' && src.charAt(pos) <= '9') {
                pos++;
            }
        }
        String numStr = src.substring(start, pos);
        if (isFloat) {
            try {
                return new JsonPrimitive(Double.parseDouble(numStr));
            } catch (NumberFormatException ignored) {}
        } else {
            try {
                return new JsonPrimitive(Long.parseLong(numStr));
            } catch (NumberFormatException ignored) {}
        }
        return new JsonPrimitive(numStr);
    }

    private void consumeKeyword(String kw) {
        int kwLen = kw.length();
        if (pos + kwLen <= len && src.substring(pos, pos + kwLen).equalsIgnoreCase(kw)) {
            pos += kwLen;
        }
    }

    private void skipWhitespace() {
        while (pos < len) {
            char c = src.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                pos++;
            } else {
                break;
            }
        }
    }
}
