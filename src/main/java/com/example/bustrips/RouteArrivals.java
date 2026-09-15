package com.example.bustrips;

import java.time.ZonedDateTime;
import java.util.List;

public record RouteArrivals(String route, List<ZonedDateTime> arrivals) {
}
