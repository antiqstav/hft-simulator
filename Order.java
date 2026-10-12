public class Order {

    // Order has a unique orderID
    //           a price at which it is placed (1002 = $10.02, 000 = $0.00, 10020 = $100.20)
    //           a quantity of how much is being ordered
    //           an enum of if it is a bid or ask (BID = buy, ASK = sell)
    //           and a timestamp indicating when it was created (msec from market open)

    // not changed after initialization of order
    final private long orderID;
    final private long price;
    final private OrderType type;
    final private long timestamp;

    private int quantity;

    public enum OrderType {
        BID,
        ASK
    }

    public Order(long orderID, long price, int quantity, OrderType type, long timestamp) {
        this.orderID = orderID;
        this.price = price;
        this.quantity = quantity;
        this.type = type;
        this.timestamp = timestamp;
    }

    // accessors
    public long getOrderID() { return orderID; }

    public long getPrice() { return price; }

    public int getQuantity() { return quantity; }

    public OrderType getType() { return type; }

    public long getTimestamp() { return timestamp; }

    // mutator
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}