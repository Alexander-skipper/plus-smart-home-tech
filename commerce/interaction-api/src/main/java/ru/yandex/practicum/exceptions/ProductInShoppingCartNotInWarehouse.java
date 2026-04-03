package ru.yandex.practicum.exceptions;

public class ProductInShoppingCartNotInWarehouse extends RuntimeException {
    private String userMessage;

    public ProductInShoppingCartNotInWarehouse() {}

    public ProductInShoppingCartNotInWarehouse(String message) {
        super(message);
        this.userMessage = message;
    }

    public ProductInShoppingCartNotInWarehouse(String message, String userMessage) {
        super(message);
        this.userMessage = userMessage;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}