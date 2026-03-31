package ru.yandex.practicum.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.exceptions.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoOrderFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoOrderFoundException(NoOrderFoundException ex) {
        log.warn("Заказ не найден: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", ex.getUserMessage());
        response.put("httpStatus", HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(NoProductsInShoppingCartException.class)
    public ResponseEntity<Map<String, Object>> handleNoProductsInShoppingCartException(NoProductsInShoppingCartException ex) {
        log.warn("Корзина пуста: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", ex.getUserMessage());
        response.put("httpStatus", HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouse.class)
    public ResponseEntity<Map<String, Object>> handleProductInShoppingCartLowQuantityInWarehouse(
            ProductInShoppingCartLowQuantityInWarehouse ex) {
        log.warn("Недостаточно товаров на складе: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", ex.getUserMessage());
        response.put("httpStatus", HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotAuthorizedUserException.class)
    public ResponseEntity<Map<String, Object>> handleNotAuthorizedUserException(NotAuthorizedUserException ex) {
        log.warn("Неавторизованный пользователь: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", ex.getUserMessage());
        response.put("httpStatus", HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Неверное состояние: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", ex.getMessage());
        response.put("httpStatus", HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Непредвиденная ошибка: {}", ex.getMessage(), ex);
        Map<String, String> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", "Внутренняя ошибка сервера");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}