package ru.yandex.practicum.exceptions;

public class NoOrderFoundException extends RuntimeException {
    private String userMessage;

    public NoOrderFoundException() {}

    public NoOrderFoundException(String message) {
        super(message);
        this.userMessage = message;
    }

    public NoOrderFoundException(String message, String userMessage) {
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