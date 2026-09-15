package com.example.bustrips;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArrivalFormatTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Riyadh");

    private static ZonedDateTime at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(ZONE);
    }

    // absolutno kot HH:mm
    @Test
    void absoluteShowsClockTime() {
        assertEquals("12:10", ArrivalFormat.ABSOLUTE.render(
                at("2020-03-02T12:10:00"), at("2020-03-02T12:00:00")));
    }

    // absolutno čez polnoč
    @Test
    void absoluteShowsTheNextDayForArrivalsPastMidnight() {
        assertEquals("00:30", ArrivalFormat.ABSOLUTE.render(
                at("2020-03-03T00:30:00"), at("2020-03-02T23:30:00")));
    }

    // relativno v celih minutah
    @Test
    void relativeCountsWholeMinutes() {
        assertEquals("10min", ArrivalFormat.RELATIVE.render(
                at("2020-03-02T12:10:00"), at("2020-03-02T12:00:00")));
    }

    // minute odreže, ne zaokroži
    @Test
    void relativeTruncatesRatherThanRounds() {
        assertEquals("9min", ArrivalFormat.RELATIVE.render(
                at("2020-03-02T12:09:59"), at("2020-03-02T12:00:00")));
    }

    // relativno čez polnoč
    @Test
    void relativeSpansMidnight() {
        assertEquals("60min", ArrivalFormat.RELATIVE.render(
                at("2020-03-03T00:30:00"), at("2020-03-02T23:30:00")));
    }

    // velike ali male črke
    @ParameterizedTest
    @ValueSource(strings = {"RELATIVE", "Absolute", "relative", "absolute"})
    void parseIsCaseInsensitive(String value) {
        ArrivalFormat.parse(value);
    }

    // neznan format vrže izjemo
    @ParameterizedTest
    @ValueSource(strings = {"", "sideways", "rel"})
    void parseRejectsAnythingElse(String value) {
        assertThrows(IllegalArgumentException.class, () -> ArrivalFormat.parse(value));
    }
}
