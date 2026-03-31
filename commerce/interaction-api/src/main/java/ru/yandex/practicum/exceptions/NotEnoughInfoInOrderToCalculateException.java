package ru.yandex.practicum.exceptions;

public class NotEnoughInfoInOrderToCalculateException extends RuntimeException {
    private String userMessage;

    public NotEnoughInfoInOrderToCalculateException() {}

    public NotEnoughInfoInOrderToCalculateException(String message) {
        super(message);
        this.userMessage = message;
    }

    public NotEnoughInfoInOrderToCalculateException(String message, String userMessage) {
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