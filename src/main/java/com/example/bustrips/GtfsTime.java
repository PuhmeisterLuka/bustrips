package com.example.bustrips;

import java.time.Duration;

public final class GtfsTime {
    private GtfsTime() {
    }

    // ure niso omejene: GTFS za vožnjo čez polnoč zapiše 25:30:00
    public static Duration parse(String value) {
        String[] parts = value == null ? new String[0] : value.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Expected HH:MM:SS but got \"" + value + "\"");
        }
        return Duration.ofHours(Parsers.parseInt(parts[0], 0, Integer.MAX_VALUE, "hours"))
                .plusMinutes(Parsers.parseInt(parts[1], 0, 59, "minutes"))
                .plusSeconds(Parsers.parseInt(parts[2], 0, 59, "seconds"));
    }
}
