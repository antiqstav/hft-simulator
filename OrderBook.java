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

    public enum Status {
        APPLIED,
        FALLBACK,
        UNRESOLVED
    }

    public OrderBook() {
        // sorted lowest price as the first
        asks = new TreeMap<>();
        // sorted highest price as the first
        bids = new TreeMap<>(Collections.reverseOrder());
        orderIndex = new HashMap<>();
    }

    // starting orderbook with first message
    public boolean seed(String row, int levels) {
        String[] f = row.split(",");
        int id = -1;

        if (levels * 4 != f.length) {
            throw new IllegalArgumentException("Expected " + levels * 4 + " columns, but received " + f.length + " columns");
        }

        for (int i = 0; i < f.length; i+=4) {
            long askPrice = Long.parseLong(f[i].trim());
            int askSize = Integer.parseInt(f[i + 1].trim());
            long bidPrice = Long.parseLong(f[i + 2].trim());
            int bidSize = Integer.parseInt(f[i + 3].trim());

            if (askSize > 0) {
                applyRestOrder(new Order(id--, askPrice, askSize, Order.Side.ASK, Order.OrderType.LIMIT, 0));
            }
            if (bidSize > 0) {
                applyRestOrder(new Order(id--, bidPrice, bidSize, Order.Side.BID, Order.OrderType.LIMIT, 0));
            }
        }

        return true;
    }

    // order handling
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

    // replay methods
    // 1 (NEW)
    public Status applyRestOrder(Order o) {
        if (orderIndex.containsKey(o.getOrderID())) {
            throw new IllegalStateException("Duplicated order ID of " + o.getOrderID());
        }
        TreeMap<Long, PriceLevel> book = (o.getSide() == Order.Side.BID) ? bids : asks;
        book.computeIfAbsent(o.getPrice(), k -> new PriceLevel()).addOrder(o);
        orderIndex.put(o.getOrderID(), o);
        return Status.APPLIED;
    }
    // 2 (PARTIAL_CANCEL) and 4 (EXECUTE_VISIBLE)
    public Status applyReduce(long orderID, int quantity, Order.Side side, long price) {
        if (quantity <= 0) throw new IllegalArgumentException("Reduce amount must be positive " + quantity);
        if (orderIndex.containsKey(orderID)) {
            Order o = orderIndex.get(orderID);
            if (o.getSide() != side) throw new IllegalStateException("Message does not have correct side: " + o.getSide() + " is not " + side);
            if (o.getPrice() != price) throw new IllegalStateException("Price does not match: " + o.getPrice() + " is not " + price);
            
            if (o.getQuantity() > quantity) {
                TreeMap<Long, PriceLevel> book = (o.getSide() == Order.Side.BID) ? bids : asks;
                book.get(o.getPrice()).reduce(o, o.getQuantity() - quantity);
                return Status.APPLIED;
            }
            else if (o.getQuantity() == quantity) {
                cancelOrder(orderID);
                return Status.APPLIED;
            }
            else {
                throw new IllegalStateException("Order " + orderID + " has " + o.getQuantity() + " but message removes " + quantity);
            }
        }
        else {
            return fallback(quantity, side, price);
        }
    }
    // 3 (DELETE)
    public Status applyDelete(long orderID, int size, Order.Side side, long price) {
        if (orderIndex.get(orderID) == null) {
            return fallback(size, side, price);
        }
        Order o = orderIndex.get(orderID);
        if (o.getSide() != side) throw new IllegalStateException("Message does not have correct side: " + o.getSide() + " is not " + side);
        if (o.getPrice() != price) throw new IllegalStateException("Price does not match: " + o.getPrice() + " is not " + price);
        cancelOrder(orderID);
        return Status.APPLIED;
    }
    // fallback for missing ids
    public Status fallback(int quantity, Order.Side side, long price) {
        TreeMap<Long, PriceLevel> book = (side == Order.Side.BID) ? bids : asks;
        PriceLevel pl = book.get(price);
        if (pl == null || pl.getTotalQuantity() < quantity)
            return Status.UNRESOLVED;

        int remaining = quantity;
        while (remaining > 0) {
            Order front = pl.peekOrder();
            int take = Math.min(remaining, front.getQuantity());
            pl.fill(take);
            if (front.getQuantity() == 0) orderIndex.remove(front.getOrderID());
            remaining -= take;
        }
        if (pl.isEmpty()) book.remove(price);
        return Status.FALLBACK;
    }

    // unit test methods to check orderbook follows rules
    public void checkInvariants() {
        int ordersInLevels = 0;
        for (var side : List.of(bids, asks)) {
            for (var entry : side.entrySet()) {
                PriceLevel pl = entry.getValue();
                if (pl.isEmpty()) throw new IllegalStateException("Empty level at " + entry.getKey());
                int sum = 0;
                for (Order o : pl.getOrders()) {
                    if (o.getQuantity() <= 0) throw new IllegalStateException("Bad quantity on order " + o.getOrderID());
                    sum += o.getQuantity();
                    ordersInLevels++;
                }
                if (sum != pl.getTotalQuantity())
                    throw new IllegalStateException("Total drift at " + entry.getKey());
            }
        }
        if (ordersInLevels != orderIndex.size())
            throw new IllegalStateException("Index has " + orderIndex.size() + " orders, levels have " + ordersInLevels);
        if (!bids.isEmpty() && !asks.isEmpty() && bids.firstKey() >= asks.firstKey())
            throw new IllegalStateException("Crossed book");
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
    public long[] levelAt(Order.Side side, int k) {
        TreeMap<Long, PriceLevel> book = (side == Order.Side.BID) ? bids : asks;
        int currentPriceLevel = 1;
        for (var entry : book.entrySet()) {
            if (currentPriceLevel == k) {
                return new long[]{entry.getKey(), entry.getValue().getTotalQuantity()};
            }
            currentPriceLevel++;
        }
        return null;
    }

    @Override
    public String toString() {
        return bids + "\n" + asks;
    }
}