public class Trade {

    private final long tradeID;
    private final long askOrderId;
    private final long bidOrderId;
    private final long price;
    private final long quantity;
    private final long timestamp;

    public Trade(long tradeID, long askOrderId, long bidOrderId,
                 long price, long quantity, long timestamp) {
        this.tradeID = tradeID;
        this.askOrderId = askOrderId;
        this.bidOrderId = bidOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public long getTradeID()    { return tradeID; }
    public long getAskOrderId() { return askOrderId; }
    public long getBidOrderId() { return bidOrderId; }
    public long getPrice()      { return price; }
    public long getQuantity()   { return quantity; }
    public long getTimestamp()  { return timestamp; }

    @Override
    public String toString() {
        return "TRADE #" + tradeID + ": " + quantity + " @ " + price
             + " (ask #" + askOrderId + ", bid #" + bidOrderId + ")";
    }
}