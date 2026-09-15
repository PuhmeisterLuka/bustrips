package com.example.bustrips;

import java.util.stream.Collectors;

public final class BoardPrinter {
    private static final String GAP = "   ";

    private BoardPrinter() {
    }

    public static String render(StopBoard board, ArrivalFormat format) {
        StringBuilder out = new StringBuilder(board.stop().name()).append("\n\n");
        if (board.routes().isEmpty()) {
            return out.append("No arrivals in the next ").append(ArrivalBoard.WINDOW.toHours()).append(" hours.\n")
                    .toString();
        }

        // najdaljša oznaka, za poravnavo stolpcev
        int width = board.routes().stream().mapToInt(route -> route.route().length()).max().orElse(0);
        for (RouteArrivals route : board.routes()) {
            String times = route.arrivals().stream()
                    .map(arrival -> format.render(arrival, board.from()))
                    .collect(Collectors.joining(GAP));
            out.append(route.route())
                    .append(" ".repeat(width - route.route().length()))
                    .append(GAP)
                    .append(times)
                    .append('\n');
        }
        return out.toString();
    }
}
