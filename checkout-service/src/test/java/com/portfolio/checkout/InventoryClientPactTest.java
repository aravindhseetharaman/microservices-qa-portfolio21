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
    V4Pact itemOutOfStock(PactDslWithProvider builder) {
        return builder
                .given("item 2 exists but is out of stock")
                .uponReceiving("a request for item 2, which is out of stock")
                    .path("/items/2")
                .method("GET")
                .willRespondWith()
                    .status(200)
                    .matchHeader("Content-Type", "application/json(;.*)?", "application/json")
                    .body(new PactDslJsonBody()
                            .integerType("id", 2)
                            .stringType("name", "Mouse")
                            .integerType("stock", 0))
                .toPact(V4Pact.class);
    }


    @Pact(consumer = "CheckoutService")
    V4Pact itemOutOfStock2(PactDslWithProvider builder) {
        return builder
                .given("item 3 exists but is out of stock")
                .uponReceiving("a request for item 3, which is out of stock")
                    .path("/items/3")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .matchHeader("Content-Type", "application/json(;.*)?", "application/json")
                    .body(new PactDslJsonBody()
                            .integerType("id", 3)
                            .stringType("name", "Monitor")
                            .integerType("stock", 0))
                .toPact(V4Pact.class);
    }

    @Pact(consumer = "CheckoutService")
    V4Pact itemInStock(PactDslWithProvider builder) {
        return builder
                .given("item 1 exists and is in stock")
                .uponReceiving("a request for item 1, which is in stock")
                    .path("/items/1")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .matchHeader("Content-Type", "application/json(;.*)?", "application/json")
                    .body(new PactDslJsonBody()
                            .integerType("id", 1)
                            .stringType("name", "Keyboard")
                            .integerType("stock", 25))
                .toPact(V4Pact.class);
    }

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
        //ok

    }

    @Test
    @PactTestFor (pactMethod = "itemInStock")
    void returnsItemWhenInStock(MockServer mockServer) {
        Optional<Item> item = new InventoryClient(mockServer.getUrl()).getItem(1);
        assertThat(item).isPresent();
        assertThat(item.get().id()).isEqualTo(1);
        assertThat(item.get().name()).isEqualTo("Keyboard");
        assertThat(item.get().inStock()).isTrue();
    }

    @Test
    @PactTestFor (pactMethod = "itemOutOfStock")
    void returnsItemWhenOutOfStock(MockServer mockServer) {
        Optional<Item> item = new InventoryClient(mockServer.getUrl()).getItem(2);
        assertThat(item).isPresent();
        assertThat(item.get().id()).isEqualTo(2);
        assertThat(item.get().name()).isEqualTo("Mouse");
        assertThat(item.get().inStock()).isFalse();
    }

    @Test
    @PactTestFor(pactMethod = "itemOutOfStock2")
    void returnsItemWhenOutOfStock2(MockServer mockServer) {
        Optional<Item> item = new InventoryClient(mockServer.getUrl()).getItem(3);
        assertThat(item).isPresent();
        assertThat(item.get().id()).isEqualTo(3);
        assertThat(item.get().name()).isEqualTo("Monitor");
        assertThat(item.get().inStock()).isFalse();
    }

}
