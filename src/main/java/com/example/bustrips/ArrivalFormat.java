package com.example.bustrips;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public enum ArrivalFormat {
    ABSOLUTE,
    RELATIVE;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    public static ArrivalFormat parse(String value) {
        return Parsers.parse(value, text -> valueOf(text.toUpperCase(Locale.ROOT)), "relative or absolute");
    }

    public String render(ZonedDateTime arrival, ZonedDateTime from) {
        return switch (this) {
            case ABSOLUTE -> CLOCK.format(arrival);
            // odrezano navzdol, "5min" pomeni vsaj pet minut
            case RELATIVE -> Duration.between(from, arrival).toMinutes() + "min";
        };
    }
}
