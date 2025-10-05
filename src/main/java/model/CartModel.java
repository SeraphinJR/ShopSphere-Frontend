package model;

import org.json.JSONObject;
import java.util.*;
import java.util.function.Consumer;

public class CartModel {
    private final List<JSONObject> items = new ArrayList<>();
    private final List<Consumer<Void>> listeners = new ArrayList<>();

    // Add item
    public void addItem(JSONObject item) {
        for(JSONObject obj:items){
            if(item.getInt("productId")==obj.getInt("productId")){
                obj.put("quantity",obj.getInt("quantity")+1);
                notifyListeners();
                return;
            }
        }
        items.add(item);
        notifyListeners();
    }

    // Remove item
    public void removeItem(JSONObject item) {
        items.remove(item);
        notifyListeners();
    }

    // Get a copy of items
    public List<JSONObject> getItems() {
        return new ArrayList<>(items);
    }

    // Listen for updates
    public void addChangeListener(Consumer<Void> listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Consumer<Void> listener : listeners) {
            listener.accept(null);
        }
    }

    // Clear cart
    public void clear() {
        items.clear();
        notifyListeners();
    }
}
