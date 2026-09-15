package com.example.bustrips;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public final class Main {
    private static final int OK = 0;
    private static final int FEED_ERROR = 1;
    private static final int BAD_USAGE = 2;
    private static final int NO_SUCH_STOP = 3;

    private static final String USAGE = """
            Usage: busTrips <station_id> <num_buses_per_line> <relative|absolute> [reference_time]

              station_id           stop_id or stop_code from stops.txt
              num_buses_per_line   maximum arrivals to show per route
              relative|absolute    "10min" or "12:10"
              reference_time       optional, ISO local date-time, e.g. 2020-03-02T07:00

            The feed directory is taken from -Dgtfs.dir, then $GTFS_DIR, then the working directory.""";

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args)); // run() sam ne konča procesa, zato ga testi lahko kličejo
    }

    static int run(String[] args) {
        if (args.length < 3 || args.length > 4) {
            System.err.println(USAGE);
            return BAD_USAGE;
        }

        try {
            String stopId = args[0];
            int perRoute = Parsers.parseInt(args[1], 1, Integer.MAX_VALUE, "num_buses_per_line");
            ArrivalFormat format = ArrivalFormat.parse(args[2]);
            ZonedDateTime now = args.length == 4 ? referenceTime(args[3]) : ZonedDateTime.now();

            GtfsFeed feed = new GtfsFeed(feedDirectory());
            Optional<StopBoard> board = new ArrivalBoard(feed).at(stopId, perRoute, now);
            if (board.isEmpty()) {
                System.err.println("No stop with id or code \"" + stopId + "\" in " + feed.directory());
                return NO_SUCH_STOP;
            }
            System.out.print(BoardPrinter.render(board.get(), format));
            return OK;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return BAD_USAGE;
        } catch (IOException e) {
            System.err.println("Could not read the feed: " + e.getMessage());
            return FEED_ERROR;
        }
    }

    // neobvezen 4. argument: za potekel feed in ponovljive teste
    private static ZonedDateTime referenceTime(String value) {
        return Parsers.parse(value, LocalDateTime::parse, "reference_time like 2020-03-02T07:00")
                .atZone(ZoneId.systemDefault());
    }

    private static Path feedDirectory() {
        String configured = System.getProperty("gtfs.dir", System.getenv("GTFS_DIR"));
        return Path.of(configured == null ? "." : configured);
    }
}
