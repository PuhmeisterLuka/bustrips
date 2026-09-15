package com.example.bustrips;

import java.util.function.Function;

final class Parsers {
    private Parsers() {
    }

    // vsako napako razčlenjevanja pretvori v berljivo IllegalArgumentException
    static <T> T parse(String value, Function<String, T> parser, String expected) {
        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Expected " + expected + " but got \"" + value + "\"", e);
        }
    }

    static int parseInt(String value, int min, int max, String name) {
        int parsed = parse(value, text -> Integer.parseInt(text.trim()), "a whole number for " + name);
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(name + " is out of range: " + parsed);
        }
        return parsed;
    }
}
