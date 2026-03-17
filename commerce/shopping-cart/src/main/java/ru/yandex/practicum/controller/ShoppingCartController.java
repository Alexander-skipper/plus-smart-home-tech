package ru.yandex.practicum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.ShoppingCartClient;
import ru.yandex.practicum.client.WarehouseServiceClient;
import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.entity.ShoppingCart;
import ru.yandex.practicum.exceptions.NoProductsInShoppingCartException;
import ru.yandex.practicum.exceptions.NotAuthorizedUserException;
import ru.yandex.practicum.exceptions.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.repository.ShoppingCartRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-cart")
public class ShoppingCartController implements ShoppingCartClient {

    private final ShoppingCartRepository cartRepository;
    private final WarehouseServiceClient warehouseClient;

    @Autowired
    public ShoppingCartController(ShoppingCartRepository cartRepository,
                                  WarehouseServiceClient warehouseClient) {
        this.cartRepository = cartRepository;
        this.warehouseClient = warehouseClient;
    }

    @Override
    @GetMapping
    public ShoppingCartDto getShoppingCart(@RequestParam("username") String username) {
        validateUsername(username);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseGet(() -> createNewCart(username));

        return convertToDto(cart);
    }

    @Override
    @PutMapping
    public ShoppingCartDto addProductToShoppingCart(
            @RequestParam("username") String username,
            @RequestBody Map<UUID, Long> products) {

        validateUsername(username);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseGet(() -> createNewCart(username));

        if (!cart.isActive()) {
            throw new NotAuthorizedUserException(
                    "Корзина деактивирована");
        }

        ShoppingCartDto cartForCheck = new ShoppingCartDto(cart.getShoppingCartId(), products);
        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(cartForCheck);
        } catch (ProductInShoppingCartLowQuantityInWarehouse ex) {
            throw ex;
        }

        products.forEach((productId, quantity) -> {
            cart.getProducts().merge(productId, quantity, Long::sum);
        });

        ShoppingCart savedCart = cartRepository.save(cart);
        return convertToDto(savedCart);
    }

    @Override
    @DeleteMapping
    public void deactivateCurrentShoppingCart(@RequestParam("username") String username) {
        validateUsername(username);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new NotAuthorizedUserException(
                        "Корзина не найдена"));

        cart.setActive(false);
        cartRepository.save(cart);
    }

    @Override
    @PostMapping("/remove")
    public ShoppingCartDto removeFromShoppingCart(
            @RequestParam("username") String username,
            @RequestBody List<UUID> productIds) {

        validateUsername(username);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new NotAuthorizedUserException(
                        "Корзина не найдена"));

        if (!cart.isActive()) {
            throw new NotAuthorizedUserException(
                    "Корзина деактивирована");
        }

        boolean anyRemoved = false;
        for (UUID productId : productIds) {
            if (cart.getProducts().remove(productId) != null) {
                anyRemoved = true;
            }
        }

        if (!anyRemoved) {
            throw new NoProductsInShoppingCartException(
                    "Указанные товары не найдены в корзине");
        }

        ShoppingCart savedCart = cartRepository.save(cart);
        return convertToDto(savedCart);
    }

    @Override
    @PostMapping("/change-quantity")
    public ShoppingCartDto changeProductQuantity(
            @RequestParam("username") String username,
            @RequestBody ChangeProductQuantityRequest request) {

        validateUsername(username);

        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new NotAuthorizedUserException(
                        "Корзина не найдена"));

        if (!cart.isActive()) {
            throw new NotAuthorizedUserException(
                    "Корзина деактивирована");
        }

        if (!cart.getProducts().containsKey(request.getProductId())) {
            throw new NoProductsInShoppingCartException(
                    "Товар не найден в корзине");
        }

        Map<UUID, Long> singleProductMap = Map.of(request.getProductId(), request.getNewQuantity());
        ShoppingCartDto cartForCheck = new ShoppingCartDto(cart.getShoppingCartId(), singleProductMap);

        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(cartForCheck);
        } catch (ProductInShoppingCartLowQuantityInWarehouse ex) {
            throw ex;
        }

        cart.getProducts().put(request.getProductId(), request.getNewQuantity());
        ShoppingCart savedCart = cartRepository.save(cart);
        return convertToDto(savedCart);
    }

    private void validateUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new NotAuthorizedUserException(
                    "Имя пользователя не должно быть пустым");
        }
    }

    private ShoppingCart createNewCart(String username) {
        ShoppingCart cart = new ShoppingCart();
        cart.setUsername(username);
        cart.setActive(true);
        return cartRepository.save(cart);
    }

    private ShoppingCartDto convertToDto(ShoppingCart cart) {
        return new ShoppingCartDto(
                cart.getShoppingCartId(),
                cart.getProducts()
        );
    }
}