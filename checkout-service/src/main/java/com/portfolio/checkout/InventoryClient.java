package com.portfolio.checkout;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

/** HTTP client for InventoryService. This is the code the Pact consumer test exercises. */
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    /** Returns the item, or empty if InventoryService responds 404. */
    public Optional<Item> getItem(long id) {
        return restClient.get()
                .uri("/items/{id}", id)
                .exchange((request, response) -> {
                    if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                        return Optional.empty();
                    }
                    if (response.getStatusCode().isError()) {
                        throw new IllegalStateException("InventoryService returned " + response.getStatusCode());
                    }
                    return Optional.ofNullable(response.bodyTo(Item.class));
                });
    }
}
