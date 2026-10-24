public class OrderbookValidator {

    public static String firstMismatch(OrderBook book, String row, int levels) {
        long[] v = parseRow(row, levels);

        for (int k = 1; k <= levels; k++) {
            int c = 4 * (k - 1);   // ask price, ask size, bid price, bid size

            String ask = checkLevel("ASK", k, book.levelAt(Order.Side.ASK, k), v[c], v[c + 1]);
            if (ask != null) return ask;

            String bid = checkLevel("BID", k, book.levelAt(Order.Side.BID, k), v[c + 2], v[c + 3]);
            if (bid != null) return bid;
        }
        return null;
    }

    private static String checkLevel(String side, int k, long[] mine, long expPrice, long expSize) {
        if (mine == null) {
            return (expSize == 0) ? null
                : side + " level " + k + ": expected " + expPrice + " x " + expSize + ", book has no level";
        }
        if (expSize == 0)
            return side + " level " + k + ": expected empty, book has " + mine[0] + " x " + mine[1];
        if (mine[0] != expPrice || mine[1] != expSize)
            return side + " level " + k + ": expected " + expPrice + " x " + expSize
                 + ", book has " + mine[0] + " x " + mine[1];
        return null;
    }

    private static long[] parseRow(String row, int levels) {
        String[] f = row.split(",");
        // if (f.length != 4 * levels)
        //     throw new IllegalArgumentException("Expected " + (4 * levels) + " columns, got " + f.length);
        long[] v = new long[f.length];
        for (int i = 0; i < f.length; i++) v[i] = Long.parseLong(f[i].trim());
        return v;
    }
}