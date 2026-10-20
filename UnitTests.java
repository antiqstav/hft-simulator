import java.util.Random;
public class UnitTests {

    static int passed = 0, failed = 0;
    static final int[] SIZES = {100, 150, 200};

    static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.out.println("FAIL: " + name); }
    }

    static void checkEq(String name, long expected, long actual) {
        if (expected == actual) passed++;
        else { failed++; System.out.println("FAIL: " + name + " expected " + expected + " got " + actual); }
    }

    static void checkThrows(String name, String line) {
        try {
            Message.parseLine(line);
            failed++;
            System.out.println("FAIL: " + name + " (no exception)");
        } catch (RuntimeException e) {
            passed++;
        }
    }

    public static void main(String[] args) {
        nanosTests();
        fieldTests();
        eventTypeTests();
        sideTests();
        formattingTests();
        badInputTests();
        for (int i = 0; i < 500; i++) {
            randomInvariantTests(i, 1000);
        }
        System.out.println(passed + " passed, " + failed + " failed");
    }

    // checks for digit precision
    static void nanosTests() {
        checkEq("9 digit fraction", 34200015105074L, Message.parseNanos("34200.015105074"));
        checkEq("6 digit fraction pads right", 34200059902000L, Message.parseNanos("34200.059902"));
        checkEq("no decimal point", 34200000000000L, Message.parseNanos("34200"));
        checkEq("1 digit fraction", 34200500000000L, Message.parseNanos("34200.5"));
        checkEq("smallest fraction", 34200000000001L, Message.parseNanos("34200.000000001"));
        checkEq("over 9 digits truncates", 34200123456789L, Message.parseNanos("34200.1234567891"));
        checkEq("leading-zero fraction", 34200000001000L, Message.parseNanos("34200.000001"));
    }

    // general tests
    static void fieldTests() {
        Message m = Message.parseLine("34200.015105074,4,15818974,4,5794000,1");
        checkEq("id", 15818974L, m.getOrderID());
        checkEq("size", 4, m.getSize());
        checkEq("price", 5794000L, m.getPrice());
        checkEq("time", 34200015105074L, m.getTimestamp());
        check("type", m.getEventType() == Message.EventType.EXECUTE_VISIBLE);
        check("side", m.getSide() == Order.Side.BID);

        Message s = Message.parseLine("34748.511890,1,27942247,100,5762700,-1");
        checkEq("sell id", 27942247L, s.getOrderID());
        checkEq("sell size", 100, s.getSize());
        checkEq("sell price", 5762700L, s.getPrice());
        checkEq("sell time", 34748511890000L, s.getTimestamp());
        check("sell type", s.getEventType() == Message.EventType.NEW);
        check("sell side", s.getSide() == Order.Side.ASK);

        // an id too big for an int must survive
        Message big = Message.parseLine("34200.0,1,2147483648,100,5794000,1");
        checkEq("id beyond int range", 2147483648L, big.getOrderID());
    }

    // checks for event type confirmation
    static void eventTypeTests() {
        check("code 1", Message.parseLine("34200.0,1,1,1,100,1").getEventType() == Message.EventType.NEW);
        check("code 2", Message.parseLine("34200.0,2,1,1,100,1").getEventType() == Message.EventType.PARTIAL_CANCEL);
        check("code 3", Message.parseLine("34200.0,3,1,1,100,1").getEventType() == Message.EventType.DELETE);
        check("code 4", Message.parseLine("34200.0,4,1,1,100,1").getEventType() == Message.EventType.EXECUTE_VISIBLE);
        check("code 5", Message.parseLine("34200.0,5,0,1,100,1").getEventType() == Message.EventType.EXECUTE_HIDDEN);

        Message h = Message.parseLine("36023,7,0,0,-1,-1");   // halt row from the readme
        check("halt type", h.getEventType() == Message.EventType.HALT);
        checkEq("halt price is a status code", -1L, h.getPrice());
        checkEq("halt size", 0, h.getSize());
        checkEq("halt time", 36023000000000L, h.getTimestamp());

        Message hid = Message.parseLine("34200.113247,5,0,1,5795100,1");
        checkEq("hidden id is 0", 0L, hid.getOrderID());
    }

    // side test
    static void sideTests() {
        check("direction 1 is BID", Message.parseLine("34200.0,1,1,1,100,1").getSide() == Order.Side.BID);
        check("direction -1 is ASK", Message.parseLine("34200.0,1,1,1,100,-1").getSide() == Order.Side.ASK);
    }

    static void formattingTests() {
        Message m = Message.parseLine(" 34200.059902 , 4 , 15818974 , 300 , 5794000 , 1 ");
        checkEq("spaces: id", 15818974L, m.getOrderID());
        checkEq("spaces: time", 34200059902000L, m.getTimestamp());
        check("spaces: side", m.getSide() == Order.Side.BID);

        Message crlf = Message.parseLine("34200.059902,4,15818974,300,5794000,1\r");   // Windows line ending
        check("CRLF side", crlf.getSide() == Order.Side.BID);
    }

    static void badInputTests() {
        checkThrows("unknown code 6", "34200.0,6,1,1,100,1");
        checkThrows("unknown code 0", "34200.0,0,1,1,100,1");
        checkThrows("5 fields", "34200.0,4,1,1,100");
        checkThrows("7 fields", "34200.0,4,1,1,100,1,9");
        checkThrows("empty line", "");
        checkThrows("non-numeric id", "34200.0,4,abc,1,100,1");
        checkThrows("non-numeric time", "abc,4,1,1,100,1");
        checkThrows("size too big for int", "34200.0,4,1,3000000000,100,1");
        // Known gap: direction 0 currently becomes ASK silently.
        // This fails until you add direction validation for types 1-5.
        checkThrows("direction 0 on a NEW order", "34200.0,1,1,1,100,0");
    }

    static void randomInvariantTests(long seed, int iterations) {
        Random rng = new Random(seed);
        OrderBook ob = new OrderBook();
        long nextId = 0;

        for (int i = 0; i < iterations; i++) {
            int size = SIZES[rng.nextInt(3)];
            Order.Side side = rng.nextBoolean() ? Order.Side.ASK : Order.Side.BID;
            long price = 995 + rng.nextInt(11);

            try {
                int nextMove = rng.nextInt(10);
                if (nextMove < 6) {
                    ob.addOrder(new Order(nextId++, price, size, side, Order.OrderType.LIMIT, i)); //add liquidity
                }
                else if (nextMove < 7) {
                    ob.addOrder(new Order(nextId++, price, size, side, Order.OrderType.MARKET, i)); //remove liquidity
                }
                else if (nextMove < 9) {
                    ob.cancelOrder(rng.nextInt((int) Math.max(1, nextId)));
                } else {
                    ob.modifyOrder(rng.nextInt((int) Math.max(1, nextId)), price, size, i);
                }
                ob.checkInvariants();
            }
            catch (Exception e) {
                System.out.println("FAILED: seed=" + seed + " op=" + i);
                throw e;
            }
        }
        if (ob.getTradesCompleted().isEmpty()) {
            throw new IllegalStateException("seed " + seed + ": no trades happened, test proves nothing");
        }
    }
}