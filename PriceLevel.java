import java.util.ArrayDeque;

public class PriceLevel {

    private int totalQuantity;
    private ArrayDeque<Order> orders = new ArrayDeque<>();
    private long price;

    public PriceLevel() {
        totalQuantity = 0;
        price = 0;
    }
    public PriceLevel(Order o) {
        orders.add(o);
        totalQuantity = o.getQuantity();
        price = o.getPrice();
    }

    public void fill(int quantity) {
        Order front = orders.peek();
        if (front == null) {
            System.out.println("No orders to fill at " + price);
            return;
        }
        if (quantity <= 0 || quantity > front.getQuantity()) {
            System.out.println("Invalid fill " + quantity + " for front order with " + front.getQuantity());
            return;
        }
        front.setQuantity(front.getQuantity() - quantity);
        totalQuantity -= quantity;
        if (front.getQuantity() == 0) {
            orders.poll();
        }
    }

    public void addOrder(Order o) {
        orders.add(o);
        totalQuantity += o.getQuantity();
    }

    public Order peekOrder() {
        return orders.peek();
    }

    public boolean removeOrder(Order o) {
        boolean removed = orders.remove(o);
        if (removed) {
            totalQuantity -= o.getQuantity();
        }
        return removed;
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public void setPrice(long price) {
        this.price = price;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public long getPrice() {
        return price;
    }

    public String toString() {
        return "" +totalQuantity;
    }
}