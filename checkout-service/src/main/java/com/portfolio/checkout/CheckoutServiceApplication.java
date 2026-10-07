package com.portfolio.checkout;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CheckoutServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckoutServiceApplication.class, args);
    }

    @Bean
    InventoryClient inventoryClient(@Value("${inventory.base-url:http://localhost:8081}") String baseUrl) {
        return new InventoryClient(baseUrl);
    }
}
