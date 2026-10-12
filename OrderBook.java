import java.util.Map;
import java.util.TreeMap;
import java.util.Collections;
import java.util.HashMap;

public class OrderBook {

    // OrderBook has a TreeMap of buy orders
    //               a TreeMap of sell orders

    private TreeMap<Long, PriceLevel> bids;
    private TreeMap<Long, PriceLevel> asks;
    private HashMap<Long, Order> orderIndex;

    public OrderBook() {
        // sorted lowest price as the first
        asks = new TreeMap<>();
        // sorted highest price as the first
        bids = new TreeMap<>(Collections.reverseOrder());
        orderIndex = new HashMap<>();
    }

    public void addOrder (Order o) {
        Order.OrderType type = o.getType();
        if (type == Order.OrderType.BID) {
            while (o.getQuantity() > 0 && !asks.isEmpty() && asks.firstKey() <= o.getPrice()) {
                PriceLevel pl = asks.get(asks.firstKey());
                Order currentOrder = pl.peekOrder();
                int fillQuantity = Math.min(o.getQuantity(), currentOrder.getQuantity());
                System.out.println("TRADE COMPLETED: " + currentOrder.getPrice() + " at " + fillQuantity + " (resting order #" + currentOrder.getOrderID() + ", incoming order #" + o.getOrderID() + ")");
                pl.fill(fillQuantity);
                o.setQuantity(o.getQuantity() - fillQuantity);
                if (currentOrder.getQuantity() == 0) {
                    orderIndex.remove(currentOrder.getOrderID());
                }
                if (pl.isEmpty()) {
                    asks.remove(asks.firstKey());
                }
            }
            if (o.getQuantity() > 0) {
                bids.computeIfAbsent(o.getPrice(), k -> new PriceLevel()).addOrder(o);
                orderIndex.put(o.getOrderID(), o);
            }
        }
        else {
            while (o.getQuantity() > 0 && !bids.isEmpty() && bids.firstKey() >= o.getPrice()) {
                PriceLevel pl = bids.get(bids.firstKey());
                Order currentOrder = pl.peekOrder();
                int fillQuantity = Math.min(o.getQuantity(), currentOrder.getQuantity());
                System.out.println("TRADE COMPLETED: " + currentOrder.getPrice() + " at " + fillQuantity + " (resting order #" + currentOrder.getOrderID() + ", incoming order #" + o.getOrderID() + ")");
                pl.fill(fillQuantity);
                o.setQuantity(o.getQuantity() - fillQuantity);
                if (currentOrder.getQuantity() == 0) {
                    orderIndex.remove(currentOrder.getOrderID());
                }
                if (pl.isEmpty()) {
                    bids.remove(bids.firstKey());
                }
            }
            if (o.getQuantity() > 0) {
                asks.computeIfAbsent(o.getPrice(), k -> new PriceLevel()).addOrder(o);
                orderIndex.put(o.getOrderID(), o);
            }
        }
    }

    public void cancelOrder(long orderID) {
        Order o = orderIndex.get(orderID);
        if (o == null) {
            System.out.println("Order not found: " + orderID);
            return;
        }
        Order.OrderType type = o.getType();
        TreeMap<Long, PriceLevel> book = (type == Order.OrderType.BID) ? bids : asks;
        PriceLevel pl = book.get(o.getPrice());
        if (pl != null) {
            pl.removeOrder(o);
            if (pl.isEmpty()) {
                book.remove(o.getPrice());
            }
        }
        orderIndex.remove(orderID);
    }

    // accessors
    public TreeMap<Long, PriceLevel> getBids() {
        return bids;
    }
    public TreeMap<Long, PriceLevel> getAsks() {
        return asks;
    }
    public long bestBid() { return bids.isEmpty() ? 0 : bids.firstKey(); }
    public long bestAsk() { return asks.isEmpty() ? 0 : asks.firstKey(); }

    public String toString() {
        return bids + "\n" + asks;
    }
}