package com.example.bustrips;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public record ServiceDayWindow(LocalDate serviceDate, ZoneId zone, Duration earliest, Duration latest) {
    // GTFS ne omeji, kako daleč čez polnoč sega servisni dan
    private static final Duration MAX_SERVICE_TIME = Duration.ofHours(32);

    public static List<ServiceDayWindow> covering(ZonedDateTime start, Duration length) {
        ZonedDateTime end = start.plus(length);
        ZoneId zone = start.getZone();
        List<ServiceDayWindow> windows = new ArrayList<>();

        // začne dan prej: včerajšnja vožnja morda še vozi
        for (LocalDate day = start.toLocalDate().minusDays(1); !day.isAfter(end.toLocalDate()); day = day.plusDays(1)) {
            ZonedDateTime midnight = day.atStartOfDay(zone);
            Duration earliest = Duration.between(midnight, start);
            Duration latest = Duration.between(midnight, end);
            if (latest.isNegative() || earliest.compareTo(MAX_SERVICE_TIME) > 0) {
                continue;
            }
            // časi prihodov niso nikoli negativni
            windows.add(new ServiceDayWindow(day, zone, earliest.isNegative() ? Duration.ZERO : earliest, latest));
        }
        return List.copyOf(windows);
    }

    public boolean covers(Duration arrivalTime) {
        return arrivalTime.compareTo(earliest) >= 0 && arrivalTime.compareTo(latest) <= 0;
    }

    // petek + 25:30 je sobota ob 01:30
    public ZonedDateTime momentOf(Duration arrivalTime) {
        return serviceDate.atStartOfDay(zone).plus(arrivalTime);
    }
}
