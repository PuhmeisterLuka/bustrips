package com.example.bustrips;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusTripsIT {
    private static final String FEED_PROPERTY = "gtfs.dir";

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private String originalFeed;

    @BeforeEach
    void redirectOutput() throws URISyntaxException {
        originalOut = System.out;
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8)); // ujame, kar Main izpiše

        Path feed = Path.of(BusTripsIT.class.getResource("/mini-feed").toURI());
        originalFeed = System.setProperty(FEED_PROPERTY, feed.toString());
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
        if (originalFeed == null) {
            System.clearProperty(FEED_PROPERTY);
        } else {
            System.setProperty(FEED_PROPERTY, originalFeed);
        }
    }

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    // po linijah, vikend linija izpade, omejitev N
    @Test
    void showsUpcomingArrivalsGroupedByRoute() {
        int status = Main.run(new String[]{"1", "2", "absolute", "2020-03-02T07:00"});

        assertEquals(0, status);
        assertEquals("""
                Central Station

                1    07:10   07:40
                10   07:05
                """, output());
    }

    // relativni izpis
    @Test
    void countsMinutesWhenAskedForRelativeTimes() {
        int status = Main.run(new String[]{"1", "2", "relative", "2020-03-02T07:00"});

        assertEquals(0, status);
        assertEquals("""
                Central Station

                1    10min   40min
                10   5min
                """, output());
    }

    // čez polnoč: 24:30 ponedeljek, 01:00 torek
    @Test
    void looksAtTwoServiceDaysWhenTheWindowCrossesMidnight() {
        int status = Main.run(new String[]{"1", "5", "absolute", "2020-03-02T23:30"});

        assertEquals(0, status);
        assertEquals("""
                Central Station

                1   23:45   00:30
                2   01:00
                """, output());
    }

    // iskanje po stop_code
    @Test
    void acceptsAStopCodeAsWellAsAStopId() {
        int status = Main.run(new String[]{"1001", "1", "absolute", "2020-03-02T07:00"});

        assertEquals(0, status);
        assertEquals("""
                Central Station

                1    07:10
                10   07:05
                """, output());
    }

    // samo prihodi iskane postaje
    @Test
    void showsOnlyTheArrivalsOfTheStopThatWasAskedFor() {
        int status = Main.run(new String[]{"2", "3", "absolute", "2020-03-02T07:00"});

        assertEquals(0, status);
        assertEquals("""
                Museum

                1   07:25
                """, output());
    }

    // prazna tabla ob 03:00
    @Test
    void reportsAStopThatHasNothingComing() {
        int status = Main.run(new String[]{"1", "3", "absolute", "2020-03-02T03:00"});

        assertEquals(0, status);
        assertEquals("""
                Central Station

                No arrivals in the next 2 hours.
                """, output());
    }

    // neznana postaja vrne 3
    @Test
    void failsWithADistinctStatusForAnUnknownStop() {
        assertEquals(3, Main.run(new String[]{"does-not-exist", "3", "absolute", "2020-03-02T07:00"}));
        assertEquals("", output());
    }

    // napačni argumenti vrnejo 2
    @Test
    void failsWithADistinctStatusForBadArguments() {
        assertEquals(2, Main.run(new String[]{"1", "3"}));
        assertEquals(2, Main.run(new String[]{"1", "0", "absolute"}));
        assertEquals(2, Main.run(new String[]{"1", "3", "sideways"}));
        assertEquals(2, Main.run(new String[]{"1", "3", "absolute", "yesterday"}));
        assertEquals("", output());
    }
}
