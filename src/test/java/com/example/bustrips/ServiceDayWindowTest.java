package com.example.bustrips;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceDayWindowTest {
    private static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");
    private static final Duration TWO_HOURS = Duration.ofHours(2);

    private static final LocalDate FRIDAY = LocalDate.of(2020, 2, 21); // petek
    private static final LocalDate SATURDAY = LocalDate.of(2020, 2, 22);

    private static ZonedDateTime at(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(RIYADH);
    }

    private static Duration hm(int hours, int minutes) {
        return Duration.ofHours(hours).plusMinutes(minutes);
    }

    // sredi dneva en servisni dan
    @Test
    void middleOfTheDayNeedsOnlyOneServiceDay() {
        List<ServiceDayWindow> days = ServiceDayWindow.covering(at("2020-02-21T10:00:00"), TWO_HOURS);

        assertEquals(1, days.size());
        ServiceDayWindow only = days.get(0);
        assertEquals(FRIDAY, only.serviceDate());
        assertEquals(hm(10, 0), only.earliest());
        assertEquals(hm(12, 0), only.latest());
    }

    // okno čez polnoč, dva dneva
    @Test
    void windowCrossingMidnightSplitsAcrossTwoServiceDays() {
        List<ServiceDayWindow> days = ServiceDayWindow.covering(at("2020-02-21T23:30:00"), TWO_HOURS);

        assertEquals(2, days.size());

        assertEquals(FRIDAY, days.get(0).serviceDate());
        assertEquals(hm(23, 30), days.get(0).earliest());
        assertEquals(hm(25, 30), days.get(0).latest());

        assertEquals(SATURDAY, days.get(1).serviceDate());
        assertEquals(Duration.ZERO, days.get(1).earliest());
        assertEquals(hm(1, 30), days.get(1).latest());
    }

    // po polnoči še včerajšnji dan
    @Test
    void shortlyAfterMidnightStillLooksBackAtYesterdaysServiceDay() {
        List<ServiceDayWindow> days = ServiceDayWindow.covering(at("2020-02-22T00:30:00"), TWO_HOURS);

        assertEquals(2, days.size());

        assertEquals(FRIDAY, days.get(0).serviceDate());
        assertEquals(hm(24, 30), days.get(0).earliest());
        assertEquals(hm(26, 30), days.get(0).latest());

        assertEquals(SATURDAY, days.get(1).serviceDate());
        assertEquals(hm(0, 30), days.get(1).earliest());
        assertEquals(hm(2, 30), days.get(1).latest());
    }

    // meje so vključene
    @Test
    void boundsAreInclusiveAtBothEnds() {
        ServiceDayWindow friday = ServiceDayWindow.covering(at("2020-02-21T23:30:00"), TWO_HOURS).get(0);

        assertTrue(friday.covers(hm(23, 30)));
        assertTrue(friday.covers(hm(25, 30)));
        assertTrue(friday.covers(hm(24, 45)));
        assertFalse(friday.covers(hm(23, 29)));
        assertFalse(friday.covers(hm(25, 31)));
    }

    // petek 25:30 je sobota 01:30
    @Test
    void arrivalPastMidnightResolvesToTheFollowingCalendarDay() {
        ServiceDayWindow friday = ServiceDayWindow.covering(at("2020-02-21T23:30:00"), TWO_HOURS).get(0);

        assertEquals(at("2020-02-22T01:30:00"), friday.momentOf(GtfsTime.parse("25:30:00")));
    }

    // 00:45 in 24:45 v različnih dneh
    @Test
    void theTwoServiceDayWindowsAreDisjointForATwoHourQuery() {
        List<ServiceDayWindow> days = ServiceDayWindow.covering(at("2020-02-21T23:30:00"), TWO_HOURS);

        assertTrue(days.get(0).covers(hm(24, 45)));
        assertFalse(days.get(1).covers(hm(24, 45)));

        assertFalse(days.get(0).covers(hm(0, 45)));
        assertTrue(days.get(1).covers(hm(0, 45)));
    }
}
