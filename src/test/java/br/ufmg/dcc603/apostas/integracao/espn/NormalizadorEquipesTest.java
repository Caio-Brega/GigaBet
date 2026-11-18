package br.ufmg.dcc603.apostas.integracao.espn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NormalizadorEquipesTest {

    @Test
    void normalizaNomesConhecidosDoBrasileirao() {
        assertEquals("Atlético-MG", NormalizadorEquipes.normalizar("Atlético Mineiro"));
        assertEquals("Atlético-MG", NormalizadorEquipes.normalizar("atletico mineiro"));
        assertEquals("Atlético-MG", NormalizadorEquipes.normalizar("Atlético-MG"));
        assertEquals("Athletico-PR", NormalizadorEquipes.normalizar("Athletico Paranaense"));
        assertEquals("Vasco", NormalizadorEquipes.normalizar("Vasco da Gama"));
        assertEquals("Vasco", NormalizadorEquipes.normalizar("CR Vasco da Gama"));
        assertEquals("Bragantino", NormalizadorEquipes.normalizar("Red Bull Bragantino"));
        assertEquals("São Paulo", NormalizadorEquipes.normalizar("São Paulo FC"));
        assertEquals("Grêmio", NormalizadorEquipes.normalizar("Gremio FBPA"));
        assertEquals("Cruzeiro", NormalizadorEquipes.normalizar("Cruzeiro EC"));
        assertEquals("Flamengo", NormalizadorEquipes.normalizar("CR Flamengo"));
    }

    @Test
    void mantemNomeOriginalSeNaoHouverAlias() {
        assertEquals("Real Madrid", NormalizadorEquipes.normalizar("Real Madrid"));
        assertEquals("", NormalizadorEquipes.normalizar("   "));
        assertEquals("", NormalizadorEquipes.normalizar(null));
    }
}
