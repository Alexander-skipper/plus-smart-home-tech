package ru.yandex.practicum.exceptions;

public class ProductNotFoundException extends RuntimeException {
    private String userMessage;

    public ProductNotFoundException() {
    }

    public ProductNotFoundException(String message) {
        super(message);
        this.userMessage = message;
    }

    public ProductNotFoundException(String message, String userMessage) {
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