import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;

public class OrderBook {

    // OrderBook has a TreeMap of buy orders
    //               a TreeMap of sell orders

    private final TreeMap<Long, PriceLevel> bids;
    private final TreeMap<Long, PriceLevel> asks;
    private final HashMap<Long, Order> orderIndex;
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
        Order.Side type = o.getSide();
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

    public boolean cancelOrder(long orderID) {
        Order o = orderIndex.get(orderID);
        if (o == null) {
            return false;
        }
        Order.Side type = o.getSide();
        TreeMap<Long, PriceLevel> book = (type == Order.Side.BID) ? bids : asks;
        PriceLevel pl = book.get(o.getPrice());
        if (pl != null) {
            pl.removeOrder(o);
            if (pl.isEmpty()) {
                book.remove(o.getPrice());
            }
        }
        orderIndex.remove(orderID);
        return true;
    }

    public boolean modifyOrder(long orderID, long newPrice, int newQuantity, long timestamp) {
        Order o = orderIndex.get(orderID);
        if (o == null) {
            return false;
        }
        if (newQuantity <= 0) {
            cancelOrder(orderID);
            return false;
        }
        if (newPrice == o.getPrice() && newQuantity <= o.getQuantity()) {
            TreeMap<Long, PriceLevel> book;
            if (o.getSide() == Order.Side.BID) {
                book = bids;
            } else {
                book = asks;
            }
            book.get(o.getPrice()).reduce(o, newQuantity);
            return true;
        }
        cancelOrder(orderID);
        Order newOrder = new Order(orderID, newPrice, newQuantity, o.getSide(), o.getOrderType(), timestamp);
        addOrder(newOrder);
        return true;
    }

    public void checkInvariants() {
        int ordersInLevels = 0;
        for (var side : List.of(bids, asks)) {
            for (var entry : side.entrySet()) {
                PriceLevel pl = entry.getValue();
                if (pl.isEmpty()) throw new IllegalStateException("empty level at " + entry.getKey());
                int sum = 0;
                for (Order o : pl.getOrders()) {          // needs a read-only accessor on PriceLevel
                    if (o.getQuantity() <= 0) throw new IllegalStateException("bad quantity on order " + o.getOrderID());
                    sum += o.getQuantity();
                    ordersInLevels++;
                }
                if (sum != pl.getTotalQuantity())
                    throw new IllegalStateException("total drift at " + entry.getKey());
            }
        }
        if (ordersInLevels != orderIndex.size())
            throw new IllegalStateException("index has " + orderIndex.size() + " orders, levels have " + ordersInLevels);
        if (!bids.isEmpty() && !asks.isEmpty() && bids.firstKey() >= asks.firstKey())
            throw new IllegalStateException("crossed book");
    }

    // accessors
    public TreeMap<Long, PriceLevel> getBids() {
        return bids;
    }
    public TreeMap<Long, PriceLevel> getAsks() {
        return asks;
    }
    public List<Trade> getTradesCompleted() { return tradesCompleted; }
    public long bestBid(int a) {
        if (bids.isEmpty()) {
            throw new NoSuchElementException("No bids in the current price level");
        }
        return bids.firstKey();
    }
    public long bestAsk() {
        if (asks.isEmpty()) {
            throw new NoSuchElementException("No asks in the current price level");
        }
        return asks.firstKey();
    }

    @Override
    public String toString() {
        return bids + "\n" + asks;
    }
}