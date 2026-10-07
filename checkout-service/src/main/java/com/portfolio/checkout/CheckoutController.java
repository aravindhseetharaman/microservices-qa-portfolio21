package com.portfolio.checkout;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final InventoryClient inventoryClient;

    public CheckoutController(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @GetMapping("/checkout/{itemId}/availability")
    public ResponseEntity<Map<String, Object>> availability(@PathVariable long itemId) {
        return inventoryClient.getItem(itemId)
                .map(item -> ResponseEntity.ok(Map.<String, Object>of(
                        "itemId", item.id(),
                        "name", item.name(),
                        "available", item.inStock())))
                .orElse(ResponseEntity.notFound().build());
    }
}
