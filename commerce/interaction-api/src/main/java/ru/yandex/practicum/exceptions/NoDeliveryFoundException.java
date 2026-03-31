package ru.yandex.practicum.exceptions;

public class NoDeliveryFoundException extends RuntimeException {
    private String userMessage;

    public NoDeliveryFoundException() {}

    public NoDeliveryFoundException(String message) {
        super(message);
        this.userMessage = message;
    }

    public NoDeliveryFoundException(String message, String userMessage) {
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