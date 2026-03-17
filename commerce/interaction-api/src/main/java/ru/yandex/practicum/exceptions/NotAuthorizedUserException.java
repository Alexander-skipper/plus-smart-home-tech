package ru.yandex.practicum.exceptions;

public class NotAuthorizedUserException extends RuntimeException {
    private String userMessage;

    public NotAuthorizedUserException() {
    }

    public NotAuthorizedUserException(String message) {
        super(message);
        this.userMessage = message;
    }

    public NotAuthorizedUserException(String message, String userMessage) {
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