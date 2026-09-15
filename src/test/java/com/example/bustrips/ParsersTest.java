package com.example.bustrips;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParsersTest {

    // vrne rezultat razčlenjevalnika
    @Test
    void returnsWhatTheParserProduces() {
        assertEquals(LocalDate.of(2020, 3, 2), Parsers.parse("2020-03-02", LocalDate::parse, "a date"));
    }

    // napaka postane IllegalArgumentException
    @Test
    void turnsAnyParserFailureIntoIllegalArgument() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Parsers.parse("soon", LocalDate::parse, "a date"));
        assertEquals("Expected a date but got \"soon\"", e.getMessage());
    }

    // število znotraj meja
    @Test
    void parsesAnIntegerInsideItsRange() {
        assertEquals(5, Parsers.parseInt(" 5 ", 0, 59, "minutes"));
        assertEquals(0, Parsers.parseInt("0", 0, 59, "minutes"));
        assertEquals(59, Parsers.parseInt("59", 0, 59, "minutes"));
    }

    // število zunaj meja
    @Test
    void rejectsAnIntegerOutsideItsRange() {
        assertThrows(IllegalArgumentException.class, () -> Parsers.parseInt("60", 0, 59, "minutes"));
        assertThrows(IllegalArgumentException.class, () -> Parsers.parseInt("-1", 0, 59, "minutes"));
        assertThrows(IllegalArgumentException.class, () -> Parsers.parseInt("five", 0, 59, "minutes"));
    }
}
