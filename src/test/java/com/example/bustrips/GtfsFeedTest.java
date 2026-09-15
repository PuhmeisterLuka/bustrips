package com.example.bustrips;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsFeedTest {

    private static final ZoneId RIYADH = ZoneId.of("Asia/Riyadh");
    private static final LocalDate FRIDAY = LocalDate.of(2020, 2, 21);
    private static final LocalDate SATURDAY = LocalDate.of(2020, 2, 22);

    @TempDir
    Path dir;

    private GtfsFeed feedWith(String fileName, String content) throws IOException {
        Files.writeString(dir.resolve(fileName), content, StandardCharsets.UTF_8);
        return new GtfsFeed(dir);
    }

    private static List<ServiceDayWindow> fridayNightWindow() {
        ZonedDateTime now = LocalDateTime.parse("2020-02-21T23:30:00").atZone(RIYADH);
        return ServiceDayWindow.covering(now, Duration.ofHours(2));
    }

    private static List<String> tripIds(List<StopArrival> arrivals) {
        return arrivals.stream().map(StopArrival::tripId).toList();
    }

    // postaja po stop_id
    @Test
    void findsAStopById() throws IOException {
        GtfsFeed feed = feedWith("stops.txt", """
                stop_id,stop_code,stop_name
                2,2001,Clock Roundabout
                """);

        assertEquals(Optional.of(new Stop("2", "Clock Roundabout")), feed.findStop("2"));
    }

    // po stop_code vrne stop_id
    @Test
    void findsAStopByCodeAndReturnsItsId() throws IOException {
        GtfsFeed feed = feedWith("stops.txt", """
                stop_id,stop_code,stop_name
                2,2001,Clock Roundabout
                """);

        assertEquals(Optional.of(new Stop("2", "Clock Roundabout")), feed.findStop("2001"));
    }

    // ob trku zmaga stop_id
    @Test
    void prefersIdOverCodeWhenBothMatch() throws IOException {
        GtfsFeed feed = feedWith("stops.txt", """
                stop_id,stop_code,stop_name
                7,5,Matched by code
                5,9,Matched by id
                """);

        assertEquals("Matched by id", feed.findStop("5").orElseThrow().name());
    }

    // neznana postaja
    @Test
    void reportsAnUnknownStop() throws IOException {
        GtfsFeed feed = feedWith("stops.txt", """
                stop_id,stop_name
                2,Clock Roundabout
                """);

        assertTrue(feed.findStop("99").isEmpty());
    }

    // filter po postaji in oknu
    @Test
    void keepsOnlyArrivalsAtTheStopInsideTheWindow() throws IOException {
        GtfsFeed feed = feedWith("stop_times.txt", """
                trip_id,arrival_time,departure_time,stop_id,stop_sequence
                IN_FRIDAY,23:45:00,23:45:00,2,1
                PAST_MIDNIGHT,24:45:00,24:45:00,2,2
                NEXT_SERVICE_DAY,01:00:00,01:00:00,2,1
                TOO_EARLY,10:00:00,10:00:00,2,1
                WRONG_STOP,23:45:00,23:45:00,3,1
                NO_TIMEPOINT,,23:45:00,2,1
                """);

        assertEquals(List.of("IN_FRIDAY", "PAST_MIDNIGHT", "NEXT_SERVICE_DAY"),
                tripIds(feed.arrivalsAt("2", fridayNightWindow())));
    }

    // prihod nosi svoj servisni dan
    @Test
    void attributesEachArrivalToItsOwnServiceDay() throws IOException {
        GtfsFeed feed = feedWith("stop_times.txt", """
                trip_id,arrival_time,departure_time,stop_id,stop_sequence
                PAST_MIDNIGHT,24:45:00,24:45:00,2,1
                NEXT_SERVICE_DAY,00:45:00,00:45:00,2,1
                """);

        List<StopArrival> arrivals = feed.arrivalsAt("2", fridayNightWindow());

        assertEquals(FRIDAY, arrivals.get(0).serviceDate());
        assertEquals(SATURDAY, arrivals.get(1).serviceDate());
        assertEquals(LocalDateTime.parse("2020-02-22T00:45:00").atZone(RIYADH), arrivals.get(0).arrival());
        assertEquals(arrivals.get(0).arrival(), arrivals.get(1).arrival());
    }

    // stolpci po imenu, BOM
    @Test
    void readsColumnsByNameAndToleratesAByteOrderMark() throws IOException {
        GtfsFeed feed = feedWith("stop_times.txt", "\uFEFF" + """
                stop_id,stop_sequence,arrival_time,trip_id
                2,1,23:45:00,SHUFFLED
                """);

        assertEquals(List.of("SHUFFLED"), tripIds(feed.arrivalsAt("2", fridayNightWindow())));
    }

    // manjkajoč stolpec vrže izjemo
    @Test
    void rejectsAFileMissingARequiredColumn() throws IOException {
        GtfsFeed feed = feedWith("stop_times.txt", """
                trip_id,departure_time,stop_id
                BROKEN,23:45:00,2
                """);

        assertThrows(IllegalArgumentException.class, () -> feed.arrivalsAt("2", fridayNightWindow()));
    }

    // samo iskane vožnje
    @Test
    void resolvesOnlyTheRequestedTrips() throws IOException {
        GtfsFeed feed = feedWith("trips.txt", """
                route_id,service_id,trip_id
                101,WEEKDAY,TRIP_A
                102,WEEKEND,TRIP_B
                103,WEEKDAY,TRIP_C
                """);

        Map<String, TripInfo> trips = feed.trips(Set.of("TRIP_A", "TRIP_C", "MISSING"));

        assertEquals(Map.of(
                "TRIP_A", new TripInfo("101", "WEEKDAY"),
                "TRIP_C", new TripInfo("103", "WEEKDAY")), trips);
    }

    // brez ključev ne odpre datoteke
    @Test
    void doesNotOpenAFileWhenThereIsNothingToLookUp() throws IOException {
        GtfsFeed empty = new GtfsFeed(dir);

        assertTrue(empty.trips(Set.of()).isEmpty());
        assertTrue(empty.routeLabels(Set.of()).isEmpty());
    }

    // ime linije: kratko, dolgo, id
    @Test
    void labelsARouteByTheFirstNameItHas() throws IOException {
        GtfsFeed feed = feedWith("routes.txt", """
                route_id,route_short_name,route_long_name
                A,101,Uhud Line
                B,,Quba Line
                C,,
                """);

        assertEquals(Map.of("A", "101", "B", "Quba Line", "C", "C"), feed.routeLabels(Set.of("A", "B", "C")));
    }

    // dnevi in obdobje veljavnosti
    @Test
    void readsServicePatternsAndTheirValidity() throws IOException {
        GtfsFeed feed = feedWith("calendar.txt", """
                service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
                WEEKDAY,1,1,1,1,1,0,0,20200215,20200515
                WEEKEND,0,0,0,0,0,1,1,20200215,20200515
                """);

        Map<String, ServiceCalendar> services = feed.calendars().orElseThrow();
        ServiceCalendar weekday = services.get("WEEKDAY");

        assertEquals(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), weekday.days());
        assertTrue(weekday.runsOn(LocalDate.of(2020, 3, 2)));
        assertFalse(weekday.runsOn(LocalDate.of(2020, 3, 7)));
        assertTrue(services.get("WEEKEND").runsOn(LocalDate.of(2020, 3, 7)));
        assertFalse(weekday.runsOn(LocalDate.of(2020, 2, 14)));
        assertFalse(weekday.runsOn(LocalDate.of(2020, 5, 18)));
    }

    // brez calendar.txt
    @Test
    void treatsAMissingCalendarAsAbsent() throws IOException {
        assertTrue(new GtfsFeed(dir).calendars().isEmpty());
    }
}
