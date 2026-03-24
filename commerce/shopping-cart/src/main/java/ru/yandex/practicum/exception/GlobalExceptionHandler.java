package ru.yandex.practicum.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.exceptions.NoProductsInShoppingCartException;
import ru.yandex.practicum.exceptions.NotAuthorizedUserException;
import ru.yandex.practicum.exceptions.ProductInShoppingCartLowQuantityInWarehouse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotAuthorizedUserException.class)
    public ResponseEntity<Map<String, Object>> handleNotAuthorizedUserException(NotAuthorizedUserException ex) {
        log.warn("Ошибка авторизации: пользователь не авторизован. Причина: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", ex.getUserMessage());
        response.put("httpStatus", HttpStatus.UNAUTHORIZED);

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NoProductsInShoppingCartException.class)
    public ResponseEntity<Map<String, Object>> handleNoProductsInShoppingCartException(NoProductsInShoppingCartException ex) {
        log.warn("Ошибка корзины: в корзине отсутствуют товары. Причина: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", ex.getUserMessage());
        response.put("httpStatus", HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouse.class)
    public ResponseEntity<Map<String, Object>> handleProductInShoppingCartLowQuantityInWarehouse(
            ProductInShoppingCartLowQuantityInWarehouse ex) {
        log.warn("Ошибка наличия товаров: недостаточное количество товара на складе. Причина: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", ex.getUserMessage());
        response.put("httpStatus", HttpStatus.BAD_REQUEST);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Непредвиденная ошибка в работе приложения. Тип ошибки: {}, Сообщение: {}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);
        Map<String, String> response = new HashMap<>();
        response.put("message", ex.getMessage());
        response.put("userMessage", "Внутренняя ошибка сервера");

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}