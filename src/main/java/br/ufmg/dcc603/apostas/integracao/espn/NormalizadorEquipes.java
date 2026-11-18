package br.ufmg.dcc603.apostas.integracao.espn;

import java.util.Map;

public final class NormalizadorEquipes {
    private static final Map<String, String> MAPEAMENTO = Map.ofEntries(
            Map.entry("atletico mineiro", "Atlético-MG"),
            Map.entry("atlético mineiro", "Atlético-MG"),
            Map.entry("atletico-mg", "Atlético-MG"),
            Map.entry("atlético-mg", "Atlético-MG"),
            Map.entry("athletico paranaense", "Athletico-PR"),
            Map.entry("atlético paranaense", "Athletico-PR"),
            Map.entry("atletico paranaense", "Athletico-PR"),
            Map.entry("athletico-pr", "Athletico-PR"),
            Map.entry("atletico-pr", "Athletico-PR"),
            Map.entry("atlético-pr", "Athletico-PR"),
            Map.entry("vasco da gama", "Vasco"),
            Map.entry("cr vasco da gama", "Vasco"),
            Map.entry("vasco", "Vasco"),
            Map.entry("red bull bragantino", "Bragantino"),
            Map.entry("rb bragantino", "Bragantino"),
            Map.entry("bragantino", "Bragantino"),
            Map.entry("atletico goianiense", "Atlético-GO"),
            Map.entry("atlético goianiense", "Atlético-GO"),
            Map.entry("atletico-go", "Atlético-GO"),
            Map.entry("atlético-go", "Atlético-GO"),
            Map.entry("america mineiro", "América-MG"),
            Map.entry("américa mineiro", "América-MG"),
            Map.entry("america-mg", "América-MG"),
            Map.entry("américa-mg", "América-MG"),
            Map.entry("sao paulo", "São Paulo"),
            Map.entry("são paulo", "São Paulo"),
            Map.entry("sao paulo fc", "São Paulo"),
            Map.entry("são paulo fc", "São Paulo"),
            Map.entry("gremio", "Grêmio"),
            Map.entry("grêmio", "Grêmio"),
            Map.entry("gremio fbpa", "Grêmio"),
            Map.entry("cuiaba", "Cuiabá"),
            Map.entry("cuiabá", "Cuiabá"),
            Map.entry("criciuma", "Criciúma"),
            Map.entry("criciúma", "Criciúma"),
            Map.entry("vitoria", "Vitória"),
            Map.entry("vitória", "Vitória"),
            Map.entry("ceara", "Ceará"),
            Map.entry("ceará", "Ceará"),
            Map.entry("flamengo", "Flamengo"),
            Map.entry("cr flamengo", "Flamengo"),
            Map.entry("palmeiras", "Palmeiras"),
            Map.entry("se palmeiras", "Palmeiras"),
            Map.entry("corinthians", "Corinthians"),
            Map.entry("sc corinthians paulista", "Corinthians"),
            Map.entry("cruzeiro", "Cruzeiro"),
            Map.entry("cruzeiro ec", "Cruzeiro"),
            Map.entry("internacional", "Internacional"),
            Map.entry("sc internacional", "Internacional"),
            Map.entry("santos", "Santos"),
            Map.entry("santos fc", "Santos"),
            Map.entry("fluminense", "Fluminense"),
            Map.entry("fluminense fc", "Fluminense"),
            Map.entry("botafogo", "Botafogo"),
            Map.entry("botafogo fr", "Botafogo"),
            Map.entry("bahia", "Bahia"),
            Map.entry("ec bahia", "Bahia"),
            Map.entry("fortaleza", "Fortaleza"),
            Map.entry("fortaleza ec", "Fortaleza"),
            Map.entry("juventude", "Juventude"),
            Map.entry("ec juventude", "Juventude")
    );

    private NormalizadorEquipes() {}

    public static String normalizar(String nome) {
        if (nome == null || nome.isBlank()) {
            return "";
        }
        String limpo = nome.trim();
        String chave = limpo.toLowerCase();
        return MAPEAMENTO.getOrDefault(chave, limpo);
    }
}
