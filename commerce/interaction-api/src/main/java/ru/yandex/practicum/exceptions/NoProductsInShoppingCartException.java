package ru.yandex.practicum.exceptions;

public class NoProductsInShoppingCartException extends RuntimeException {
    private String userMessage;

    public NoProductsInShoppingCartException() {
    }

    public NoProductsInShoppingCartException(String message) {
        super(message);
        this.userMessage = message;
    }

    public NoProductsInShoppingCartException(String message, String userMessage) {
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