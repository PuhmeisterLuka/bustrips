package com.example.bustrips;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GtfsTimeTest {
    // polnoč je nič
    @Test
    void parsesMidnight() {
        assertEquals(Duration.ZERO, GtfsTime.parse("00:00:00"));
    }

    // navaden čas dneva
    @Test
    void parsesOrdinaryTime() {
        Duration expected = Duration.ofHours(23).plusMinutes(46).plusSeconds(27);
        assertEquals(expected, GtfsTime.parse("23:46:27"));
    }

    // čas čez 24 ur
    @Test
    void parsesTimePastMidnight() {
        assertEquals(Duration.ofHours(25).plusMinutes(30), GtfsTime.parse("25:30:00"));
    }

    // enomestna ura
    @Test
    void parsesSingleDigitHour() {
        assertEquals(Duration.ofHours(5).plusMinutes(30), GtfsTime.parse("5:30:00"));
    }

    // napačen zapis vrže izjemo
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "12:30", "12:30:00:00", "ab:cd:ef", "-01:00:00"})
    void rejectsMalformedInput(String value) {
        assertThrows(IllegalArgumentException.class, () -> GtfsTime.parse(value));
    }

    // null vrže izjemo
    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> GtfsTime.parse(null));
    }
}
