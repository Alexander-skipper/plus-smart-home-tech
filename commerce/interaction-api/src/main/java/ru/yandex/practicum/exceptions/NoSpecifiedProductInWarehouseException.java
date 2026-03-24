package ru.yandex.practicum.exceptions;

public class NoSpecifiedProductInWarehouseException extends RuntimeException {
    private String userMessage;

    public NoSpecifiedProductInWarehouseException() {}

    public NoSpecifiedProductInWarehouseException(String message) {
        super(message);
        this.userMessage = message;
    }

    public NoSpecifiedProductInWarehouseException(String message, String userMessage) {
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