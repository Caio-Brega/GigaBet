package br.ufmg.dcc603.apostas.integracao.espn.dto;

import br.ufmg.dcc603.apostas.integracao.espn.json.JsonArray;
import br.ufmg.dcc603.apostas.integracao.espn.json.JsonElement;
import br.ufmg.dcc603.apostas.integracao.espn.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record EspnScoreboardDto(List<EspnEventDto> events) {
    public EspnScoreboardDto {
        events = events != null ? Collections.unmodifiableList(events) : List.of();
    }

    public static EspnScoreboardDto fromJson(JsonObject json) {
        if (json == null) {
            return new EspnScoreboardDto(List.of());
        }
        List<EspnEventDto> list = new ArrayList<>();
        JsonArray events = json.getArray("events");
        if (events != null) {
            for (JsonElement el : events) {
                if (el.isObject()) {
                    EspnEventDto event = EspnEventDto.fromJson(el.asObject());
                    if (event != null) {
                        list.add(event);
                    }
                }
            }
        }
        return new EspnScoreboardDto(list);
    }
}
