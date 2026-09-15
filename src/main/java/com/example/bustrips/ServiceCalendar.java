package com.example.bustrips;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record ServiceCalendar(Set<DayOfWeek> days, LocalDate from, LocalDate to) {
    public boolean runsOn(LocalDate serviceDate) {
        return !serviceDate.isBefore(from)
                && !serviceDate.isAfter(to)
                && days.contains(serviceDate.getDayOfWeek());
    }
}
