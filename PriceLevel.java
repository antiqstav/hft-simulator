import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PriceLevel {

    private int totalQuantity;
    private final ArrayDeque<Order> orders = new ArrayDeque<>();

    public PriceLevel() {
        totalQuantity = 0;
    }

    public void fill(int quantity) {
        Order front = orders.peek();
        if (front == null) {
            throw new IllegalArgumentException("Price level already empty.");
        }
        if (quantity <= 0 || quantity > front.getQuantity()) {
            throw new IllegalArgumentException("Negative quantity leftover.");
        }
        front.setQuantity(front.getQuantity() - quantity);
        totalQuantity -= quantity;
        if (front.getQuantity() == 0) {
            orders.poll();
        }
    }

    public void reduce(Order o, int newQuantity) {
        if (newQuantity <= 0 || newQuantity > o.getQuantity()) {
            throw new IllegalArgumentException("Attempted to reduce to " + newQuantity + " from " + o.getQuantity());
        }
        totalQuantity -= (o.getQuantity() - newQuantity);
        o.setQuantity(newQuantity);
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

    public int getTotalQuantity() {
        return totalQuantity;
    }
    
    public Collection<Order> getOrders() {
        List<Order> list = new ArrayList<>();
        for (Order o : orders) {
            list.add(o);
        }
        return Collections.unmodifiableCollection(list);
    }

    @Override 
    public String toString() {
        return "" + totalQuantity;
    }
}