# HFT Simulator
## Built by Stavya Palassery

> Status: work in progress. Currently a limit order book, a matching engine, and a replay driver that rebuilds the book > from LOBSTER data and checks it against the exchange's own snapshots. The simulator components (latency, >queue-position fills, strategy) are on the roadmap below.

A Java project building toward a simulator for testing market-making
strategies under realistic high-frequency conditions (latency, queue position).
It models those conditions; it is not a trading system itself.

## What works today

- Limit order book with price-time priority of two sorted maps of price to a PriorityQueue, with an order-based retrieval index
- Replay driver with LOBSTER by parsing message files, seeding the book from the first orderbook row, and matches any orders that need to be matched
- Management of matching limit/market orders as trades execute at the resting order's price, partial fills keep their place in line, and market order checks the book until fulfilled
- Randomized invariant test cases to check that the orderbook is in a valid state after each message

## Results

Replay of one hour (9:30 to 10:30) of AAPL on 2012-06-21, using the LOBSTER 50-level sample, comparing the top 10 levels:

| | |
| -------- | -------- |
| Messages in file | 91997 rows (0th row seeds the book) |
| Matched the top 10 levels | 53972 of 91996 messages |
| First mismatched message | Message with order ID 53971 |
| Applied by order ID / Fallback / Unresolved | 26562 / 42 / 23 |
| Throughput (messages/second) | 39138 msgs/sec |

The mismatch comes from the data, not necessarily from the code. See "Findings" and "Known limitations".

## How it works
### Classes/Objects in consideration:
- Order (orderID, price, quantity, side of BID/ASK, orderType of LIMIT/MARKET, timestamp) - a resting/market order that exists as a mutable entry in the Orderbook
- Message (line of message) - one parsed line of the LOBSTER messages file
- PriceLevel (ArrayDeque<Order> orderList, totalQuantity) - a queue of orders at a given price
- OrderBook (TreeMap<Long, PriceLevel> of bids and ask orders, trades completed list) - the central order book that contains resting Orders
- Trade (tradeID, askOrder and bidOrder IDs, price, quantity, timestamp when occurred) - completed trades stored as an immutable record
- Main - builds the main orderbook and handles parsing logic

### Replay vs. matching
The recorded messages are facts about what already happened, so the replay applies them directly to the book instead of running them through the matcher. That means replay tests the book's state tracking (levels, queues, totals, the index). It does not test the matching engine. The matching engine is exercised by the unit scenarios and the randomized tests, and it will matter in the simulator, when a strategy's orders enter the replayed book.

## Roadmap

- [x] Limit order book (price-time priority, matching, cancels)
- [X] Trade records instead of printing
- [X] Modify orders and market orders
- [X] Replay driver, validated against real order book snapshots (matches the top 10 levels for part of one AAPL hour, see Results)
- [ ] Latency and queue-position fill model
- [ ] Market-making strategy and a latency experiment
- [ ] Report of findings

## Results and findings

- In a partial cancel (message with type 2), the size given is the amount cancelled. In four traceable cases, it fits that reading and not the consistent "remaining size". The rest were consisten with both. In a visible exceution, the size is the amount executed, and in a deletion, the size matched the order's remaining size.
- Row order matters much more than timestamps because multiple orders share one timestamp.
- The message file announces 95.4% of all messages that actually occurred and is traceable in the orderbook.
- The file reports new orders within its price window. In the AAPL sample, all 44,226 type 1 messages made while that side showed a full 50 levels landed inside the window. A level at 5925000 appeared in the orderbook at message 506 with no message ever mentioning that price.

## Design notes
- Prices are stored as integer ticks (the data's dollars x 10,000), not doubles, to avoid floating-point comparison bugs.
- A cancel finds its order in O(1) through the index, but removing it from a level's ArrayDeque is O(n) in that level's size. A doubly linked list would make it O(1).
- The book is seeded from the first orderbook row, one synthetic order per occupied level (negative ids so they cannot collide with real ones). The first message is skipped, since row 0 already includes its effect.
- The replay works after the first seeding.
- A message for an order id the book has never seen (for example, an order resting before 9:30) takes the fallback: it reduces the level's total by the message's size, taking shares from the front of the level. If the level does not exist or is too small, the message is counted as unresolved and nothing changes.
- A message whose id is known but whose side, price, or size disagrees with the book throws, because that means the book has drifted from the data.
- Hidden executions (type 5) and halts (type 7) are counted and skipped.
- Quantities and sizes that should be impossible throw exceptions instead of printing and continuing.

## Known limitations
- Replay diverges from the data. The book matches the top 10 levels for part of the hour, then diverges because of orders the data source does not report (see Findings). Divergence tends to show up well after its cause, when a hidden level slides into view.
- Approximate starting state. The first orderbook row gives only each level's total, so queue positions for orders resting before 9:30 are guesses. The fallback also reduces whichever order is at the front of the level, which may not be the one the exchange touched. Only orders that arrive after the file starts have a trustworthy place in line.
- Replay does not test the matching engine (see "Replay vs. matching").
- Limited evidence: one stock, one hour (plus a GOOG sample for message semantics), and hidden executions are not modeled.
- No latency or fill modeling yet.
- Cancels are O(n) in level size.