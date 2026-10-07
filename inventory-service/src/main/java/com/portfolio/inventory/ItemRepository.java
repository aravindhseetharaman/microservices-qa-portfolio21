package com.portfolio.inventory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory store, seeded with a couple of items. */
@Repository
public class ItemRepository {

    private final Map<Long, Item> items = new ConcurrentHashMap<>();

    public ItemRepository() {
        save(new Item(1, "Keyboard", 25));
        save(new Item(2, "Mouse", 0));
    }

    public Optional<Item> findById(long id) {
        return Optional.ofNullable(items.get(id));
    }

    public void save(Item item) {
        items.put(item.id(), item);
    }

    public void deleteById(long id) {
        items.remove(id);
    }
}
