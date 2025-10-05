package model;

import org.json.JSONObject;
import java.util.*;
import java.util.function.Consumer;

public class CartModel {
    private final List<JSONObject> items = new ArrayList<>();
    private final List<Consumer<Void>> listeners = new ArrayList<>();

    // Add item
    public void addItem(JSONObject item) {
        String newId = item.has("productId") ? String.valueOf(item.get("productId"))
                : (item.has("id") ? String.valueOf(item.get("id")) : null);
        if (newId != null) {
            for (JSONObject obj : items) {
                String existingId = obj.has("productId") ? String.valueOf(obj.get("productId"))
                        : (obj.has("id") ? String.valueOf(obj.get("id")) : null);
                if (existingId != null && existingId.equals(newId)) {
                    int cur = obj.optInt("quantity", 1);
                    obj.put("quantity", cur + 1);
                    notifyListeners();
                    return;
                }
            }
        }
        // normalize quantity
        if (!item.has("quantity"))
            item.put("quantity", 1);
        items.add(item);
        notifyListeners();
    }

    // Remove item
    public void removeItem(JSONObject item) {
        // try remove by identity first, then by product id
        if (!items.remove(item)) {
            String remId = item.has("productId") ? String.valueOf(item.get("productId"))
                    : (item.has("id") ? String.valueOf(item.get("id")) : null);
            if (remId != null) {
                Iterator<JSONObject> it = items.iterator();
                while (it.hasNext()) {
                    JSONObject o = it.next();
                    String oid = o.has("productId") ? String.valueOf(o.get("productId"))
                            : (o.has("id") ? String.valueOf(o.get("id")) : null);
                    if (oid != null && oid.equals(remId)) {
                        it.remove();
                        break;
                    }
                }
            }
        }
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
