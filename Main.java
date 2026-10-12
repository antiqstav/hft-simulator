public class Main {
    public static void main(String[] args) {
        OrderBook clob = new OrderBook();

        Order a1 = new Order(0, 1003, 100, Order.OrderType.ASK, 1);
        Order a2 = new Order(1, 1001, 100, Order.OrderType.ASK, 2);
        Order a3 = new Order(2, 1002, 100, Order.OrderType.ASK, 3);
        Order b1 = new Order(3, 1002, 150, Order.OrderType.BID, 4);

        clob.addOrder(a1);
        clob.addOrder(a2);
        clob.addOrder(a3);
        clob.addOrder(b1);

        System.out.println(clob);
    }
}