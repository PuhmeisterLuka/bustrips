package com.example.bustrips;

import java.time.ZonedDateTime;
import java.util.List;

public record StopBoard(Stop stop, ZonedDateTime from, List<RouteArrivals> routes) {
}
