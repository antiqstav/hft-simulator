public class Message {
    
    private final long orderID;
    private final int size;
    private final EventType eventType;
    private final Order.Side side;
    private final long timestamp;
    private final long price;

    public enum EventType {
        NEW, // 1
        PARTIAL_CANCEL, // 2
        DELETE, // 3
        EXECUTE_VISIBLE, // 4
        EXECUTE_HIDDEN, // 5
        HALT, // 7 (exit)
    }

    public Message(long orderID, int size, EventType eventType, Order.Side side, long timestamp, long price) {
        this.orderID = orderID;
        this.size = size;
        this.side = side;
        this.timestamp = timestamp;
        this.price = price;
        this.eventType = eventType;
    }

    public long getOrderID() {
        return orderID;
    }

    public int getSize() {
        return size;
    }

    public Order.Side getSide() {
        return side;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getPrice() {
        return price;
    }

    public EventType getEventType() {
        return eventType;
    }

    public static Message parseLine (String line) {
        String[] items = line.split(",");
        if (items.length != 6) {
            throw new IllegalArgumentException("Incorrect amount of arguments placed");
        }
        for (int i = 0; i < items.length; i++) { items[i] = items[i].trim(); }

        int et = Integer.parseInt(items[1]);
        EventType e;
        switch (et) {
            case 1 -> e = EventType.NEW;
            case 2 -> e = EventType.PARTIAL_CANCEL;
            case 3 -> e = EventType.DELETE;
            case 4 -> e = EventType.EXECUTE_VISIBLE;
            case 5 -> e = EventType.EXECUTE_HIDDEN;
            case 7 -> e = EventType.HALT;
            default -> throw new IllegalArgumentException("Unknown event code at " + et);
        }
        Order.Side s;
        switch (Integer.parseInt(items[5])) {
            case 1 -> s = Order.Side.BID;
            case -1 -> s = Order.Side.ASK;
            default -> throw new IllegalArgumentException("Unknown side code at " + items[5]);
        }
        // long orderID, int size, EventType eventType, Order.Side side, long timestamp, long price
        return new Message(Long.parseLong(items[2]), Integer.parseInt(items[3]), e, s, parseNanos(items[0]), Long.parseLong(items[4]));
    }

    static long parseNanos(String t) {
        int dot = t.indexOf('.');
        String secPart = (dot < 0) ? t : t.substring(0, dot);
        String frac = (dot < 0) ? "" : t.substring(dot + 1);
        if (frac.length() > 9) frac = frac.substring(0, 9);
        while (frac.length() < 9) frac += "0";          // pad on the RIGHT
        return Long.parseLong(secPart) * 1_000_000_000L + Long.parseLong(frac);
    }

    @Override
    public String toString() {
        String sideName = (side == Order.Side.BID) ? "bid" : "ask";
        String prefix = "[t=" + timestamp + "ns] ";

        switch (eventType) {
            case NEW:
                prefix += "New limit " + sideName + " #" + orderID + ": " + size + " @ " + price;
                break;
            case PARTIAL_CANCEL:
                prefix += "Partial cancel of " + sideName + " #" + orderID + ": -" + size + " @ " + price;
                break;
            case DELETE:
                prefix += "Delete " + sideName + " #" + orderID + ": " + size + " @ " + price;
                break;
            case EXECUTE_VISIBLE:
                prefix += "Visible execution of resting " + sideName + " #" + orderID + ": " + size + " @ " + price;
                break;
            case EXECUTE_HIDDEN:
                prefix += "Hidden execution: " + size + " @ " + price + " (not in visible book)";
                break;
            case HALT:
                prefix += "Trading halt indicator, status code " + price;
                break;
            default:
                throw new IllegalArgumentException("Order has an invalid eventType");
        }

        return prefix;
    }
}
