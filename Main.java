public class Main {
    public static void main(String[] args) {
        OrderBook clob = new OrderBook();

        Order a1 = new Order(0, 1003, 100, Order.Side.ASK, Order.OrderType.LIMIT, 1);
        Order a2 = new Order(1, 1001, 100, Order.Side.ASK, Order.OrderType.LIMIT, 2);
        Order a3 = new Order(2, 1002, 100, Order.Side.ASK, Order.OrderType.LIMIT, 3);
        Order b1 = new Order(3, 1002, 150, Order.Side.BID, Order.OrderType.LIMIT, 4);
        Order b2 = new Order(4, 0, 200, Order.Side.BID, Order.OrderType.MARKET, 5); 

        clob.addOrder(a1);
        clob.addOrder(a2);
        clob.addOrder(a3);
        clob.addOrder(b1);

        System.out.println(clob);
        System.out.println(clob.getTradesCompleted());
    }
}