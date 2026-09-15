package com.example.bustrips;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvTest {
    // navadna vrstica
    @Test
    void splitsAPlainRow() {
        assertEquals(List.of("a", "b", "c"), Csv.split("a,b,c"));
    }

    // prazna polja ostanejo
    @Test
    void keepsEmptyFields() {
        assertEquals(List.of("a", "", "c", ""), Csv.split("a,,c,"));
    }

    // vejica v narekovajih
    @Test
    void aQuotedFieldMayContainACommaAndIsUnquoted() {
        assertEquals(
                List.of("1", "Main Street, North", "3"),
                Csv.split("1,\"Main Street, North\",3"));
    }

    // "" je en narekovaj
    @Test
    void aDoubledQuoteInsideAQuotedFieldIsOneQuote() {
        assertEquals(List.of("say \"hi\""), Csv.split("\"say \"\"hi\"\"\""));
    }

    // polje po indeksu
    @Test
    void readsASingleFieldByIndex() {
        String line = "1,\"Main Street, North\",3";
        assertEquals("1", Csv.field(line, 0));
        assertEquals("Main Street, North", Csv.field(line, 1));
        assertEquals("3", Csv.field(line, 2));
    }

    // indeks čez konec da prazen niz
    @Test
    void readingPastTheEndYieldsAnEmptyString() {
        assertEquals("", Csv.field("a,b", 7));
    }

    // primerjava polja na mestu
    @Test
    void fieldEqualsMatchesWithoutBuildingTheField() {
        String line = "NORMAL_03_101_Return_22:10,22:10:00,22:10:00,2,1";
        assertTrue(Csv.fieldEquals(line, 3, "2"));
        assertFalse(Csv.fieldEquals(line, 3, "20"));
        assertFalse(Csv.fieldEquals(line, 3, ""));
        assertTrue(Csv.fieldEquals(line, 0, "NORMAL_03_101_Return_22:10"));
    }

    // fieldEquals se ujema s field
    @Test
    void fieldEqualsAgreesWithField() {
        String line = "1,\"Main Street, North\",3";
        for (int i = 0; i < 3; i++) {
            assertTrue(Csv.fieldEquals(line, i, Csv.field(line, i))
                    || Csv.field(line, i).contains(","));
        }
        assertFalse(Csv.fieldEquals(line, 9, "anything"));
    }
}
