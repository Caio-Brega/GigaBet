package br.ufmg.dcc603.apostas.integracao.espn.dto;

import br.ufmg.dcc603.apostas.integracao.espn.json.JsonArray;
import br.ufmg.dcc603.apostas.integracao.espn.json.JsonElement;
import br.ufmg.dcc603.apostas.integracao.espn.json.JsonObject;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public record EspnEventDto(
        String id,
        Instant date,
        String statusName,
        String statusDescription,
        EspnCompetitorDto homeCompetitor,
        EspnCompetitorDto awayCompetitor,
        List<EspnDetailDto> details,
        int yellowCards) {

    public boolean isScheduled() {
        return "STATUS_SCHEDULED".equalsIgnoreCase(statusName);
    }

    public boolean isInProgress() {
        return "STATUS_IN_PROGRESS".equalsIgnoreCase(statusName)
                || "STATUS_HALFTIME".equalsIgnoreCase(statusName)
                || "STATUS_FIRST_HALF".equalsIgnoreCase(statusName)
                || "STATUS_SECOND_HALF".equalsIgnoreCase(statusName);
    }

    public boolean isFullTime() {
        return "STATUS_FULL_TIME".equalsIgnoreCase(statusName)
                || "STATUS_FINAL".equalsIgnoreCase(statusName)
                || "STATUS_FINAL_PEN".equalsIgnoreCase(statusName)
                || "STATUS_FINAL_AET".equalsIgnoreCase(statusName);
    }

    public Integer homeScore() {
        return homeCompetitor != null ? homeCompetitor.score() : null;
    }

    public Integer awayScore() {
        return awayCompetitor != null ? awayCompetitor.score() : null;
    }

    public static EspnEventDto fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }
        String id = json.getString("id", "");

        String dateStr = json.getString("date", "");
        Instant date = null;
        if (!dateStr.isBlank()) {
            try {
                date = Instant.parse(dateStr);
            } catch (DateTimeParseException e) {
                try {
                    date = OffsetDateTime.parse(dateStr).toInstant();
                } catch (Exception ignored) {}
            }
        }

        String statusName = "";
        String statusDesc = "";
        JsonObject status = json.getObject("status");
        if (status != null) {
            JsonObject type = status.getObject("type");
            if (type != null) {
                statusName = type.getString("name", "");
                statusDesc = type.getString("description", "");
            }
        }

        EspnCompetitorDto home = null;
        EspnCompetitorDto away = null;
        List<EspnDetailDto> details = new ArrayList<>();

        JsonArray competitions = json.getArray("competitions");
        if (competitions != null && !competitions.isEmpty() && competitions.get(0).isObject()) {
            JsonObject comp = competitions.getObject(0);
            JsonArray competitors = comp.getArray("competitors");
            if (competitors != null) {
                for (JsonElement cEl : competitors) {
                    if (cEl.isObject()) {
                        EspnCompetitorDto competitor = EspnCompetitorDto.fromJson(cEl.asObject());
                        if (competitor != null) {
                            if (competitor.isHome()) {
                                home = competitor;
                            } else {
                                away = competitor;
                            }
                        }
                    }
                }
            }

            JsonArray detailsArr = comp.getArray("details");
            if (detailsArr != null) {
                for (JsonElement dEl : detailsArr) {
                    if (dEl.isObject()) {
                        EspnDetailDto d = EspnDetailDto.fromJson(dEl.asObject());
                        if (d != null) {
                            details.add(d);
                        }
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

        return new EspnEventDto(id, date, statusName, statusDesc, home, away, details, ycCount);
    }
}
