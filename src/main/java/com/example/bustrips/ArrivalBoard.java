package com.example.bustrips;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ArrivalBoard {
    public static final Duration WINDOW = Duration.ofHours(2);

    private final GtfsFeed feed;

    public ArrivalBoard(GtfsFeed feed) {
        this.feed = feed;
    }

    public Optional<StopBoard> at(String stopIdOrCode, int perRoute, ZonedDateTime now) throws IOException {
        Optional<Stop> stop = feed.findStop(stopIdOrCode);
        if (stop.isEmpty()) {
            return Optional.empty();
        }

        List<StopArrival> arrivals = feed.arrivalsAt(stop.get().id(), ServiceDayWindow.covering(now, WINDOW));
        if (arrivals.isEmpty()) { // ni česa povezati, ostalih datotek ne beremo
            return Optional.of(new StopBoard(stop.get(), now, List.of()));
        }

        Map<String, TripInfo> trips = feed.trips(arrivals.stream().map(StopArrival::tripId).collect(Collectors.toSet()));
        Optional<Map<String, ServiceCalendar>> calendars = feed.calendars();

        Map<String, List<ZonedDateTime>> byRoute = new HashMap<>();
        for (StopArrival arrival : arrivals) {
            TripInfo trip = trips.get(arrival.tripId());
            // brez calendar.txt vozijo vse vožnje
            if (trip != null && calendars.map(services -> runs(services, trip, arrival)).orElse(true)) {
                byRoute.computeIfAbsent(trip.routeId(), routeId -> new ArrayList<>()).add(arrival.arrival());
            }
        }

        Map<String, String> labels = feed.routeLabels(byRoute.keySet());
        List<RouteArrivals> routes = byRoute.entrySet().stream()
                .map(entry -> new RouteArrivals(
                        labels.getOrDefault(entry.getKey(), entry.getKey()),
                        entry.getValue().stream().sorted().limit(perRoute).toList()))
                .sorted(Comparator.comparing(RouteArrivals::route, ArrivalBoard::byRouteLabel))
                .toList();

        return Optional.of(new StopBoard(stop.get(), now, routes));
    }

    private static boolean runs(Map<String, ServiceCalendar> services, TripInfo trip, StopArrival arrival) {
        ServiceCalendar service = services.get(trip.serviceId());
        return service != null && service.runsOn(arrival.serviceDate());
    }

    // številske oznake po vrednosti, da je 9 pred 10
    private static int byRouteLabel(String left, String right) {
        try {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        } catch (NumberFormatException notNumeric) {
            return left.compareTo(right);
        }
    }
}
