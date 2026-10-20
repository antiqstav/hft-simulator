public class Order {

    // Order has a unique orderID
    //           a price at which it is placed (1002 = $10.02, 000 = $0.00, 10020 = $100.20)
    //           a quantity of how much is being ordered
    //           an enum of if it is a bid or ask (BID = buy, ASK = sell)
    //           and a timestamp indicating when it was created (nanosec since midnight)

    // not changed after initialization of order
    final private long orderID;
    final private long price;
    final private Side type;
    final private OrderType orderType;
    final private long timestamp;

    private int quantity;

    public enum Side {
        BID,
        ASK
    }
    public enum OrderType {
        LIMIT,
        MARKET
    }

    public Order(long orderID, long price, int quantity, Side type, OrderType orderType, long timestamp) {
        this.orderID = orderID;
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity is below zero at " + quantity);
        }
        this.quantity = quantity;
        this.type = type;
        this.orderType = orderType;
        this.timestamp = timestamp;
        if (orderType == OrderType.LIMIT) {
            this.price = price;
        }
        else {
            if (type == Side.BID) {
                this.price = Long.MAX_VALUE;
            }
            else {
                this.price = 0;
            }
        }
    }

    // accessors
    public long getOrderID() { return orderID; }

    public long getPrice() { return price; }

    public int getQuantity() { return quantity; }

    public Side getSide() { return type; }

    public OrderType getOrderType() { return orderType; }

    public long getTimestamp() { return timestamp; }

    // mutator
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}