package br.ufmg.dcc603.apostas.integracao.espn.dto;

import br.ufmg.dcc603.apostas.integracao.espn.json.JsonArray;
import br.ufmg.dcc603.apostas.integracao.espn.json.JsonElement;
import br.ufmg.dcc603.apostas.integracao.espn.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public record EspnSummaryDto(
        String eventId,
        List<EspnDetailDto> details,
        int yellowCards) {

    public EspnSummaryDto {
        details = details != null ? List.copyOf(details) : List.of();
    }

    public static EspnSummaryDto fromJson(String eventId, JsonObject json) {
        if (json == null) {
            return new EspnSummaryDto(eventId, List.of(), 0);
        }
        List<EspnDetailDto> details = new ArrayList<>();

        // O payload varia entre os endpoints/temporadas da ESPN.
        JsonArray arr = json.getArray("details");
        if (arr == null) {
            JsonArray competitions = json.getArray("competitions");
            if (competitions != null && !competitions.isEmpty() && competitions.get(0).isObject()) {
                arr = competitions.getObject(0).getArray("details");
            }
        }
        if (arr == null) {
            JsonObject header = json.getObject("header");
            if (header != null) {
                JsonArray comps = header.getArray("competitions");
                if (comps != null && !comps.isEmpty() && comps.get(0).isObject()) {
                    arr = comps.getObject(0).getArray("details");
                }
            }
        }
        if (arr == null) {
            arr = json.getArray("keyEvents");
        }
        if (arr == null) {
            JsonObject boxscore = json.getObject("boxscore");
            if (boxscore != null) {
                arr = boxscore.getArray("details");
                if (arr == null) {
                    arr = boxscore.getArray("keyEvents");
                }
            }
        }

        if (arr != null) {
            for (JsonElement el : arr) {
                if (el.isObject()) {
                    EspnDetailDto d = EspnDetailDto.fromJson(el.asObject());
                    if (d != null) {
                        details.add(d);
                    }
                }
            }
        }

        int ycCount = 0;
        for (EspnDetailDto d : details) {
            if (d.yellowCard()) {
                ycCount++;
            }
        }

        return new EspnSummaryDto(eventId, details, ycCount);
    }
}
