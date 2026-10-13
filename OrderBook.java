import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class OrderBook {

    // OrderBook has a TreeMap of buy orders
    //               a TreeMap of sell orders

    private TreeMap<Long, PriceLevel> bids;
    private TreeMap<Long, PriceLevel> asks;
    private HashMap<Long, Order> orderIndex;
    private final List<Trade> tradesCompleted = new ArrayList<>();
    private long nextTradeID = 0;

    public OrderBook() {
        // sorted lowest price as the first
        asks = new TreeMap<>();
        // sorted highest price as the first
        bids = new TreeMap<>(Collections.reverseOrder());
        orderIndex = new HashMap<>();
    }

    public void addOrder (Order o) {
        Order.Side type = o.getType();
        Order.OrderType orderType = o.getOrderType();
        
        if (type == Order.Side.BID) {
            while (o.getQuantity() > 0 && !asks.isEmpty() && asks.firstKey() <= o.getPrice()) {
                PriceLevel pl = asks.get(asks.firstKey());
                Order currentOrder = pl.peekOrder();
                long tradePrice = currentOrder.getPrice();
                int fillQuantity = Math.min(o.getQuantity(), currentOrder.getQuantity());
                tradesCompleted.add(new Trade(nextTradeID++, currentOrder.getOrderID(), o.getOrderID(), tradePrice, fillQuantity, o.getTimestamp()));
                pl.fill(fillQuantity);
                o.setQuantity(o.getQuantity() - fillQuantity);
                if (currentOrder.getQuantity() == 0) {
                    orderIndex.remove(currentOrder.getOrderID());
                }
                if (pl.isEmpty()) {
                    asks.remove(asks.firstKey());
                }
            }
            if (o.getQuantity() > 0 && orderType == Order.OrderType.LIMIT) {
                bids.computeIfAbsent(o.getPrice(), k -> new PriceLevel()).addOrder(o);
                orderIndex.put(o.getOrderID(), o);
            }
        }
        else {
            while (o.getQuantity() > 0 && !bids.isEmpty() && bids.firstKey() >= o.getPrice()) {
                PriceLevel pl = bids.get(bids.firstKey());
                Order currentOrder = pl.peekOrder();
                long tradePrice = currentOrder.getPrice();
                int fillQuantity = Math.min(o.getQuantity(), currentOrder.getQuantity());
                tradesCompleted.add(new Trade(nextTradeID++, o.getOrderID(), currentOrder.getOrderID(), tradePrice, fillQuantity, o.getTimestamp()));
                pl.fill(fillQuantity);
                o.setQuantity(o.getQuantity() - fillQuantity);
                if (currentOrder.getQuantity() == 0) {
                    orderIndex.remove(currentOrder.getOrderID());
                }
                if (pl.isEmpty()) {
                    bids.remove(bids.firstKey());
                }
            }
            if (o.getQuantity() > 0 && orderType == Order.OrderType.LIMIT) {
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
        Order.Side type = o.getType();
        TreeMap<Long, PriceLevel> book = (type == Order.Side.BID) ? bids : asks;
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
    public List<Trade> getTradesCompleted() { return tradesCompleted; }
    public long bestBid() { return bids.isEmpty() ? 0 : bids.firstKey(); }
    public long bestAsk() { return asks.isEmpty() ? 0 : asks.firstKey(); }

    @Override
    public String toString() {
        return bids + "\n" + asks;
    }
}