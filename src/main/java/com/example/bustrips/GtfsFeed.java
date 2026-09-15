package com.example.bustrips;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

public record GtfsFeed(Path directory) {
    private static final String STOPS = "stops.txt";
    private static final String STOP_TIMES = "stop_times.txt";
    private static final String TRIPS = "trips.txt";
    private static final String ROUTES = "routes.txt";
    private static final String CALENDAR = "calendar.txt";

    public Optional<Stop> findStop(String idOrCode) throws IOException {
        try (CsvReader csv = CsvReader.open(file(STOPS))) {
            int id = csv.column("stop_id");
            int name = csv.column("stop_name");
            int code = csv.optionalColumn("stop_code");

            Stop byCode = null; // uporabi se le, če se ne ujema noben stop_id
            String line;
            while ((line = csv.nextLine()) != null) {
                if (Csv.fieldEquals(line, id, idOrCode)) {
                    return Optional.of(new Stop(idOrCode, Csv.field(line, name)));
                }
                if (byCode == null && code >= 0 && Csv.fieldEquals(line, code, idOrCode)) {
                    byCode = new Stop(Csv.field(line, id), Csv.field(line, name));
                }
            }
            return Optional.ofNullable(byCode);
        }
    }

    public List<StopArrival> arrivalsAt(String stopId, List<ServiceDayWindow> days) throws IOException {
        List<StopArrival> arrivals = new ArrayList<>();
        try (CsvReader csv = CsvReader.open(file(STOP_TIMES))) {
            int trip = csv.column("trip_id");
            int time = csv.column("arrival_time");
            int stop = csv.column("stop_id");

            String line;
            while ((line = csv.nextLine()) != null) {
                // najcenejši in najbolj selektiven pogoj najprej
                if (!Csv.fieldEquals(line, stop, stopId)) {
                    continue;
                }
                String rawTime = Csv.field(line, time);
                if (rawTime.isBlank()) { // postaja brez časovne točke
                    continue;
                }
                Duration arrivalTime = GtfsTime.parse(rawTime);
                for (ServiceDayWindow day : days) { // označi s servisnim dnem, kateremu pripada
                    if (day.covers(arrivalTime)) {
                        arrivals.add(new StopArrival(
                                Csv.field(line, trip), day.serviceDate(), day.momentOf(arrivalTime)));
                    }
                }
            }
        }
        return arrivals;
    }

    public Map<String, TripInfo> trips(Set<String> tripIds) throws IOException {
        return lookUp(TRIPS, "trip_id", tripIds, (csv, line) ->
                new TripInfo(csv.value(line, "route_id"), csv.value(line, "service_id")));
    }

    public Map<String, String> routeLabels(Set<String> routeIds) throws IOException {
        // GTFS zahteva le eno od obeh imen
        return lookUp(ROUTES, "route_id", routeIds, (csv, line) ->
                firstNonBlank(
                        csv.optionalValue(line, "route_short_name"),
                        csv.optionalValue(line, "route_long_name"),
                        csv.value(line, "route_id")));
    }

    public Optional<Map<String, ServiceCalendar>> calendars() throws IOException {
        Path path = file(CALENDAR);
        if (!Files.isReadable(path)) { // calendar.txt je neobvezen
            return Optional.empty();
        }

        Map<String, ServiceCalendar> services = new HashMap<>();
        try (CsvReader csv = CsvReader.open(path)) {
            DayOfWeek[] week = DayOfWeek.values();
            int[] weekColumns = new int[week.length]; // položaji stolpcev monday..sunday
            for (int i = 0; i < week.length; i++) {
                weekColumns[i] = csv.column(week[i].name().toLowerCase(Locale.ROOT));
            }

            String line;
            while ((line = csv.nextLine()) != null) {
                Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
                for (int i = 0; i < week.length; i++) {
                    if ("1".equals(Csv.field(line, weekColumns[i]))) {
                        days.add(week[i]);
                    }
                }
                services.put(csv.value(line, "service_id"), new ServiceCalendar(
                        days,
                        date(csv.value(line, "start_date")),
                        date(csv.value(line, "end_date"))));
            }
        }
        return Optional.of(services);
    }

    private <T> Map<String, T> lookUp(String fileName,
                                      String keyColumn,
                                      Set<String> keys,
                                      BiFunction<CsvReader, String, T> toValue) throws IOException {
        Map<String, T> found = new HashMap<>();
        if (keys.isEmpty()) {
            return found;
        }
        try (CsvReader csv = CsvReader.open(file(fileName))) {
            int key = csv.column(keyColumn);
            String line;
            while (found.size() < keys.size() && (line = csv.nextLine()) != null) { // konec, ko so najdeni vsi
                String id = Csv.field(line, key);
                if (keys.contains(id)) {
                    found.put(id, toValue.apply(csv, line));
                }
            }
        }
        return found;
    }

    private Path file(String name) {
        return directory.resolve(name);
    }

    private static LocalDate date(String value) {
        return Parsers.parse(value, text -> LocalDate.parse(text, DateTimeFormatter.BASIC_ISO_DATE),
                "a date like 20200215");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
