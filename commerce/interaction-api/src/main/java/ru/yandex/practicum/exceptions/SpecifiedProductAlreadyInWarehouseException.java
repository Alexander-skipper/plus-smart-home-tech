package ru.yandex.practicum.exceptions;

public class SpecifiedProductAlreadyInWarehouseException extends RuntimeException {
    private String userMessage;

    public SpecifiedProductAlreadyInWarehouseException() {
    }

    public SpecifiedProductAlreadyInWarehouseException(String message) {
        super(message);
        this.userMessage = message;
    }

    public SpecifiedProductAlreadyInWarehouseException(String message, String userMessage) {
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