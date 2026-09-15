package com.example.bustrips;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoardPrinterTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Riyadh");
    private static final Stop STOP = new Stop("9", "Manakha Square");

    private static ZonedDateTime at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZONE);
    }

    private static StopBoard board(RouteArrivals... routes) {
        return new StopBoard(STOP, at("2020-03-02T07:00:00"), List.of(routes));
    }

    // poravnava oznak linij
    @Test
    void alignsRouteLabelsOfDifferentWidths() {
        String rendered = BoardPrinter.render(board(
                new RouteArrivals("1", List.of(at("2020-03-02T07:10:00"))),
                new RouteArrivals("103", List.of(
                        at("2020-03-02T07:05:00"),
                        at("2020-03-02T07:20:00")))),
                ArrivalFormat.ABSOLUTE);

        assertEquals("""
                Manakha Square

                1     07:10
                103   07:05   07:20
                """, rendered);
    }

    // sporočilo brez prihodov
    @Test
    void saysSoWhenNothingIsComing() {
        assertEquals("""
                Manakha Square

                No arrivals in the next 2 hours.
                """, BoardPrinter.render(board(), ArrivalFormat.ABSOLUTE));
    }

    // relativni časi od začetka okna
    @Test
    void rendersRelativeTimesAgainstTheStartOfTheWindow() {
        String rendered = BoardPrinter.render(board(
                new RouteArrivals("101", List.of(
                        at("2020-03-02T07:03:00"),
                        at("2020-03-02T07:48:00")))),
                ArrivalFormat.RELATIVE);

        assertEquals("""
                Manakha Square

                101   3min   48min
                """, rendered);
    }
}
