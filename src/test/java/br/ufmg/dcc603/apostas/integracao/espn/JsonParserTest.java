package br.ufmg.dcc603.apostas.integracao.espn.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonParserTest {

    @Test
    void interpretaValoresPrimitivosEObjetosBasicos() {
        String json = "{\"nome\":\"Flamengo\",\"gols\":3,\"ativo\":true,\"saldo\":150.75,\"observacao\":null}";
        JsonObject obj = JsonParser.parseObject(json);

        assertEquals("Flamengo", obj.getString("nome", ""));
        assertEquals(3, obj.getInt("gols", 0));
        assertTrue(obj.getBoolean("ativo", false));
        assertEquals(150.75, obj.getDouble("saldo", 0.0), 0.001);
        assertNull(obj.getString("observacao", null));
    }

    @Test
    void interpretaEscapesEEspacosCorretamente() {
        String json = " { \"texto\": \"Linha 1\\nLinha 2\\t\\\"Aspas\\\" e barra \\\\ e unicode \\u0041\" } ";
        JsonObject obj = JsonParser.parseObject(json);

        assertEquals("Linha 1\nLinha 2\t\"Aspas\" e barra \\ e unicode A", obj.getString("texto", ""));
    }

    @Test
    void interpretaArraysEObjetosAninhados() {
        String json = """
                {
                    "eventos": [
                        {"id": "1", "status": "OK"},
                        {"id": "2", "status": "FINAL"}
                    ],
                    "contagem": 2
                }
                """;
        JsonObject obj = JsonParser.parseObject(json);
        JsonArray array = obj.getArray("eventos");
        assertNotNull(array);
        assertEquals(2, array.size());
        assertEquals("1", array.getObject(0).getString("id", ""));
        assertEquals("FINAL", array.getObject(1).getString("status", ""));
    }

    @Test
    void lidaComJsonVazioOuNuloGraciosamente() {
        JsonElement el = JsonParser.parse("");
        assertTrue(el.isNull());

        JsonElement el2 = JsonParser.parse("null");
        assertTrue(el2.isNull());
    }
}
