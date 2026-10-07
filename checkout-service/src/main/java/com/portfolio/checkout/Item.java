package com.portfolio.checkout;

/** The consumer's view of an inventory item — only the fields checkout needs. */
public record Item(long id, String name, int stock) {

    public boolean inStock() {
        return stock > 0;
    }
}
