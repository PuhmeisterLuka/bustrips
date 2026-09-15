package com.example.bustrips;

import java.util.ArrayList;
import java.util.List;

final class Csv {
    private static final char DELIMITER = ',';
    private static final char QUOTE = '"';

    private Csv() {
    }

    // cela vrstica, namenjeno glavi
    static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        int start = 0;
        while (true) {
            int end = endOf(line, start);
            fields.add(unquote(line, start, end));
            if (end == line.length()) {
                return fields;
            }
            start = end + 1;
        }
    }

    static String field(String line, int index) {
        int start = startOf(line, index);
        return start < 0 ? "" : unquote(line, start, endOf(line, start));
    }

    // primerja na mestu, neujemajoče vrstice ne ustvarijo nobenega niza
    static boolean fieldEquals(String line, int index, String expected) {
        int start = startOf(line, index);
        if (start < 0) {
            return false;
        }
        int length = endOf(line, start) - start;
        return length == expected.length() && line.regionMatches(start, expected, 0, length);
    }

    // preskoči index polj, -1 če je vrstica prekratka
    private static int startOf(String line, int index) {
        int start = 0;
        for (int i = 0; i < index; i++) {
            int end = endOf(line, start);
            if (end == line.length()) {
                return -1;
            }
            start = end + 1;
        }
        return start;
    }

    private static int endOf(String line, int start) {
        boolean quoted = false;
        for (int i = start; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == QUOTE) {
                quoted = !quoted;
            } else if (c == DELIMITER && !quoted) { // vejica v narekovajih ne loči polj
                return i;
            }
        }
        return line.length();
    }

    private static String unquote(String line, int start, int end) {
        if (end - start >= 2 && line.charAt(start) == QUOTE && line.charAt(end - 1) == QUOTE) {
            return line.substring(start + 1, end - 1).replace("\"\"", "\""); // "" pomeni en narekovaj
        }
        return line.substring(start, end);
    }
}
