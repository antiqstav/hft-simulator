import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {

    static final int LEVELS = 10;
    static final int INVARIANT_LOOP = 1000;

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.out.println("Usage: java Main <messageFile> <orderbookFile>");
            return;
        }

        OrderBook book = new OrderBook();

        long[] statusCounts = new long[3];
        long skipped = 0;
        long messageCount = 0;
        String mismatch = null;
        Message mismatchMessage = null;
        String mismatchRow = null;

        long startNanos = System.nanoTime();

        try (
            BufferedReader messageReader = new BufferedReader(new FileReader(args[0]));
            BufferedReader orderbookReader = new BufferedReader(new FileReader(args[1])); //auto closed
        ) {
            String firstMsg = messageReader.readLine();
            String firstRow = orderbookReader.readLine();

            if (firstMsg == null || firstRow == null) {
                throw new IllegalStateException("A file is empty");
            }

            book.seed(firstRow, LEVELS);
            messageCount = 1;

            String msgLine;
            String obLine;

            while ((msgLine = messageReader.readLine()) != null && (obLine = orderbookReader.readLine()) != null) {

                Message m = Message.parseLine(msgLine);

                switch (m.getEventType()) {
                    case NEW:
                        book.applyRestOrder(new Order( m.getOrderID(), m.getPrice(), m.getSize(), m.getSide(), Order.OrderType.LIMIT, m.getTimestamp()));
                        break;
                    case PARTIAL_CANCEL, EXECUTE_VISIBLE: {
                        OrderBook.Status s = book.applyReduce( m.getOrderID(), m.getSize(), m.getSide(), m.getPrice());
                        statusCounts[s.ordinal()]++;
                        break; }
                    case DELETE: {
                        OrderBook.Status s = book.applyDelete(m.getOrderID(), m.getSize(), m.getSide(), m.getPrice());
                        statusCounts[s.ordinal()]++;
                        break; }
                    case EXECUTE_HIDDEN, HALT:
                        skipped++;
                        break;
                    default:
                        throw new IllegalArgumentException("Message #" + m.getOrderID() + " has an invalid eventType");
                }

                messageCount++;
                String problem = OrderbookValidator.firstMismatch(book, obLine, LEVELS);

                if (problem != null) {
                    mismatch = problem;
                    mismatchMessage = m;
                    mismatchRow = obLine;
                    break;
                }
                if (messageCount % INVARIANT_LOOP == 0) {
                    book.checkInvariants();
                }
            }

            // If we finished without a mismatch, both files should be exhausted together.
            if (mismatch == null) {
                boolean msgsLeft = messageReader.readLine() != null;
                boolean rowsLeft = orderbookReader.readLine() != null;

                if (msgsLeft || rowsLeft) {
                    System.out.println("WARNING: files have different lengths");
                }
            }
        }

        double seconds = (System.nanoTime() - startNanos) / 1e9;

        System.out.println("Messages processed: " + messageCount);

        for (OrderBook.Status s : OrderBook.Status.values()) {
            System.out.println("  " + s + ": " + statusCounts[s.ordinal()]);
        }

        System.out.println("  skipped (hidden/halt): " + skipped);
        System.out.printf("Time: %.2fs (%.0f messages/sec)%n", seconds, messageCount / seconds);

        if (mismatch == null) {
            System.out.println("RESULT: every row matched");
        } else {
            System.out.println("RESULT: FIRST MISMATCH at message " + --messageCount);
            System.out.println("  " + mismatch);
            System.out.println("  message:  " + mismatchMessage);
            System.out.println("  expected: " + mismatchRow);
            System.out.println("  my book:");
            System.out.println(book);
        }
    }
}