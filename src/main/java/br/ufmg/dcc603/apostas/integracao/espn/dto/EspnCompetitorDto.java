package br.ufmg.dcc603.apostas.integracao.espn.dto;

import br.ufmg.dcc603.apostas.integracao.espn.json.JsonObject;

public record EspnCompetitorDto(
        String id,
        String name,
        String displayName,
        String abbreviation,
        boolean isHome,
        Integer score) {

    public static EspnCompetitorDto fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }
        String id = json.getString("id", "");
        String homeAway = json.getString("homeAway", "");
        boolean home = "home".equalsIgnoreCase(homeAway);

        String scoreStr = json.getString("score", null);
        Integer score = null;
        if (scoreStr != null && !scoreStr.isBlank()) {
            try {
                score = Integer.parseInt(scoreStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        JsonObject team = json.getObject("team");
        String name = "";
        String displayName = "";
        String abbreviation = "";
        if (team != null) {
            name = team.getString("name", "");
            displayName = team.getString("displayName", name);
            abbreviation = team.getString("abbreviation", "");
        }

        return new EspnCompetitorDto(id, name, displayName, abbreviation, home, score);
    }
}
