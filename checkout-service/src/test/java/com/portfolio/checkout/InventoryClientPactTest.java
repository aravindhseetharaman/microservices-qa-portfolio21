package com.portfolio.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Consumer contract: what CheckoutService needs from InventoryService.
 * Running this writes target/pacts/CheckoutService-InventoryService.json.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "InventoryService")
class InventoryClientPactTest {

    @Pact(consumer = "CheckoutService")
    V4Pact itemExists(PactDslWithProvider builder) {
        return builder
                .given("item 42 exists")
                .uponReceiving("a request for item 42")
                    .path("/items/42")
                        .method("GET")
                .willRespondWith()
                    .status(200)
                    .matchHeader("Content-Type", "application/json(;.*)?", "application/json")
                    .body(new PactDslJsonBody()
                            .integerType("id", 42)
                            .stringType("name", "Widget")
                            .integerType("stock", 10))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "CheckoutService")
    V4Pact itemMissing(PactDslWithProvider builder) {

        return builder
                .given("item 99 does not exist")
                .uponReceiving("a request for item 99, which does not exist")
                    .path("/items/99")
                    .method("GET")
                .willRespondWith()
                    .status(404)
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "itemExists")
    void returnsItemWhenItExists(MockServer mockServer) {
        Optional<Item> item = new InventoryClient(mockServer.getUrl()).getItem(42);
        assertThat(item).isPresent();
        assertThat(item.get().id()).isEqualTo(42);
        assertThat(item.get().name()).isEqualTo("Widget");
        assertThat(item.get().inStock()).isTrue();
    }

    @Test
    @PactTestFor(pactMethod = "itemMissing")
    void returnsEmptyWhenItemNotFound(MockServer mockServer) {
        Optional<Item> item = new InventoryClient(mockServer.getUrl()).getItem(99);

        assertThat(item).isEmpty();
    }
}
