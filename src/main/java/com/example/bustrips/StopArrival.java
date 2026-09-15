package com.example.bustrips;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public record StopArrival(String tripId, LocalDate serviceDate, ZonedDateTime arrival) {
}
