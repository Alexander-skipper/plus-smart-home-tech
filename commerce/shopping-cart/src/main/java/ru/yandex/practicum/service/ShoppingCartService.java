package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
public class ShoppingCartService {

    private final ShoppingCartRepository cartRepository;
    private final WarehouseServiceClient warehouseClient;

    @Autowired
    public ShoppingCartService(ShoppingCartRepository cartRepository,
                               WarehouseServiceClient warehouseClient) {
        this.cartRepository = cartRepository;
        this.warehouseClient = warehouseClient;
    }

    @Transactional(readOnly = true)
    public ShoppingCartDto getShoppingCart(String username) {
        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseGet(() -> createNewCart(username));
        return convertToDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Long> products) {
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

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        ShoppingCart cart = cartRepository.findByUsername(username)
                .orElseThrow(() -> new NotAuthorizedUserException(
                        "Корзина не найдена"));

        cart.setActive(false);
        cartRepository.save(cart);
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
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

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
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