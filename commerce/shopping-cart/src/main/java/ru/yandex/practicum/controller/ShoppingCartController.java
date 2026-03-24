package ru.yandex.practicum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.ShoppingCartClient;
import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.exceptions.NotAuthorizedUserException;
import ru.yandex.practicum.service.ShoppingCartService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-cart")
public class ShoppingCartController implements ShoppingCartClient {

    private final ShoppingCartService shoppingCartService;

    @Autowired
    public ShoppingCartController(ShoppingCartService shoppingCartService) {
        this.shoppingCartService = shoppingCartService;
    }

    @Override
    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam("username") String username) {
        validateUsername(username);
        return shoppingCartService.getShoppingCart(username);
    }

    @Override
    @PutMapping
    public ShoppingCartDto addProductToShoppingCart(
            @RequestParam("username") String username,
            @RequestBody Map<UUID, Long> products) {

        validateUsername(username);
        return shoppingCartService.addProductToShoppingCart(username, products);
    }

    @Override
    @DeleteMapping
    public void deactivateCurrentShoppingCart(@RequestParam("username") String username) {
        validateUsername(username);
        shoppingCartService.deactivateCurrentShoppingCart(username);
    }

    @Override
    @PostMapping("/remove")
    public ShoppingCartDto removeFromShoppingCart(
            @RequestParam("username") String username,
            @RequestBody List<UUID> productIds) {

        validateUsername(username);
        return shoppingCartService.removeFromShoppingCart(username, productIds);
    }

    @Override
    @PostMapping("/change-quantity")
    public ShoppingCartDto changeProductQuantity(
            @RequestParam("username") String username,
            @RequestBody ChangeProductQuantityRequest request) {

        validateUsername(username);
        return shoppingCartService.changeProductQuantity(username, request);
    }

    private void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new NotAuthorizedUserException(
                    "Имя пользователя не должно быть пустым");
        }
    }
}