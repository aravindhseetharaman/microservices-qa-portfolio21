package com.portfolio.inventory;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Verifies InventoryService against the consumer pacts in the Pact Broker.
 * Results are published only when -Dpact.verifier.publishResults=true.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("InventoryService")
@PactBroker(
        url = "${pactbroker.url:http://localhost:9292}",
        authentication = @PactBrokerAuth(username = "pact", password = "pact"),
        providerBranch = "${pact.provider.branch:main}",
        enablePendingPacts = "true")
class InventoryServicePactIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ItemRepository repository;

    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
                .mainBranch()
                .matchingBranch()
                .deployedOrReleased();
    }

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("item 42 exists")
    void item42Exists() {
        repository.save(new Item(42, "Widget", 10));
    }

    @State("item 99 does not exist")
    void item99DoesNotExist() {
        repository.deleteById(99);
    }
}
