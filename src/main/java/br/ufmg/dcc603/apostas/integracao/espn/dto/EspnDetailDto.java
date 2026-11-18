package br.ufmg.dcc603.apostas.integracao.espn.dto;

import br.ufmg.dcc603.apostas.integracao.espn.json.JsonObject;

public record EspnDetailDto(
        boolean yellowCard,
        String typeText,
        String typeName,
        String text,
        String teamId,
        String athleteName,
        double clock) {

    public static EspnDetailDto fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }
        boolean ycFlag = json.getBoolean("yellowCard", false);

        JsonObject type = json.getObject("type");
        String typeText = type != null ? type.getString("text", "") : "";
        String typeName = type != null ? type.getString("name", "") : "";
        String text = json.getString("text", "");

        boolean isYc = ycFlag;
        if (!isYc) {
            String lowerText = (typeText + " " + typeName + " " + text).toLowerCase();
            if (lowerText.contains("yellow card")
                    || lowerText.contains("yellow-card")
                    || lowerText.contains("cartão amarelo")
                    || lowerText.contains("cartao amarelo")
                    || lowerText.contains("yellowcard")) {
                isYc = true;
            }
        }

        JsonObject team = json.getObject("team");
        String teamId = team != null ? team.getString("id", "") : "";

        JsonObject athlete = json.getObject("athlete");
        String athleteName = athlete != null ? athlete.getString("displayName", "") : "";

        JsonObject clockObj = json.getObject("clock");
        double clock = clockObj != null ? clockObj.getDouble("value", 0.0) : 0.0;

        return new EspnDetailDto(isYc, typeText, typeName, text, teamId, athleteName, clock);
    }
}
