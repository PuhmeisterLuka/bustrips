package com.example.bustrips;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class CsvReader implements Closeable {
    private static final String BYTE_ORDER_MARK = "\uFEFF";

    private final BufferedReader reader;
    private final String fileName;
    private final List<String> header;

    private CsvReader(BufferedReader reader, String fileName, List<String> header) {
        this.reader = reader;
        this.fileName = fileName;
        this.header = header;
    }

    static CsvReader open(Path file) throws IOException {
        BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        try {
            String header = reader.readLine();
            if (header == null) {
                throw new IOException(file.getFileName() + " is empty");
            }
            if (header.startsWith(BYTE_ORDER_MARK)) { // nekateri izvozi se začnejo z BOM
                header = header.substring(1);
            }
            return new CsvReader(reader, file.getFileName().toString(), Csv.split(header));
        } catch (IOException | RuntimeException e) {
            reader.close(); // da datoteka ne ostane odprta
            throw e;
        }
    }

    int column(String name) {
        int index = optionalColumn(name);
        if (index < 0) {
            throw new IllegalArgumentException(fileName + " has no " + name + " column");
        }
        return index;
    }

    int optionalColumn(String name) {
        return header.indexOf(name);
    }

    String value(String line, String column) {
        return Csv.field(line, column(column));
    }

    // prazen niz, če stolpca v datoteki ni
    String optionalValue(String line, String column) {
        int index = optionalColumn(column);
        return index < 0 ? "" : Csv.field(line, index);
    }

    // preskoči prazne vrstice, na koncu vrne null
    String nextLine() throws IOException {
        String line;
        do {
            line = reader.readLine();
        } while (line != null && line.isEmpty());
        return line;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}
