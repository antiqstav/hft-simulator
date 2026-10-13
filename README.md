# HFT Simulator

> **Status: work in progress.** Currently a limit order book and matching
> engine in Java. The simulator components are on the roadmap below.

A Java project building toward a simulator for testing market-making
strategies under realistic high-frequency conditions (latency, queue position).
It models those conditions; it is not a trading system itself.

## What works today

- Price-time priority limit order book (two sorted maps of price → FIFO queue)
- Bids/asks matched optimized with PriorityQueue lookups
- Order cancels in O(1) lookup via an order-id index
- Best bid / best ask queries
- Differentiation of market and limit order management

## How it works

```
asks:  1002 -> [D: 500]
       1001 -> [A: 100] -> [B: 150]     <- best ask
bids:   999 -> [E: 400]                 <- best bid
```

- `Order`: id, side, price (integer ticks), quantity, timestamp
- `PriceLevel`: FIFO queue of orders at one price plus a running share total
- `OrderBook`: a bids map (high to low), an asks map (low to high), and a
  `HashMap` index from order id to order for cancels
- `Trade`: trade id, ask order id, bid order id, price, quantity traded, timestamp

Example: with asks of 300 @ 1001 and 500 @ 1002, a buy of 400 @ 1002 trades
300 @ 1001, then 100 @ 1002, leaving 400 resting at 1002.

## Running it

1. Pull the code
2. In Main.java, input all the orders as:
    Order orderName = new Order(orderID, price, quantity, Order.OrderType.ASK or BID, timestamp)
3. Add it to the Order Book as:
    clob.addOrder(orderName);
4. A sample of some orders are already in the class for you to run.
5. Run "java Main.java"

## Roadmap

- [x] Limit order book (price-time priority, matching, cancels)
- [X] Trade records instead of printing
- [X] Modify orders and market orders
- [ ] Replay driver, validated against real order book snapshots
- [ ] Latency and queue-position fill model
- [ ] Market-making strategy and a latency experiment
- [ ] Report of findings

## Design notes

- Prices are stored as integer ticks, not doubles, to avoid floating-point
  comparison bugs.
- Cancels remove from a level's queue in O(n) in that level's size. That's
  fine for now, and a doubly linked list would make it O(1).

## Known limitations

- Orders are printed rather than returned as trade objects
- No market orders, modifies, or latency modeling yet