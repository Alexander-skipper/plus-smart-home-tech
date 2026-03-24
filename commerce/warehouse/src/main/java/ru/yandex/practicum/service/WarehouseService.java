package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.entity.WarehouseItem;
import ru.yandex.practicum.exceptions.NoProductsInShoppingCartException;
import ru.yandex.practicum.exceptions.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exceptions.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.exceptions.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.repository.WarehouseRepository;

import java.util.Map;
import java.util.UUID;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Autowired
    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        validateNewProductRequest(request);

        if (warehouseRepository.existsByProductId(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Товар уже зарегистрирован на складе");
        }

        WarehouseItem item = WarehouseItem.builder()
                .productId(request.getProductId())
                .quantity(0L)
                .width(request.getDimension().getWidth())
                .height(request.getDimension().getHeight())
                .depth(request.getDimension().getDepth())
                .weight(request.getWeight())
                .fragile(request.getFragile() != null ? request.getFragile() : false)
                .build();

        warehouseRepository.save(item);
    }

    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        validateAddProductRequest(request);

        WarehouseItem item = warehouseRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                        "Товар не найден на складе"));

        item.setQuantity(item.getQuantity() + request.getQuantity());
    }

    @Transactional(readOnly = true)
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cartDto) {
        validateShoppingCart(cartDto);

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean hasFragile = false;

        for (Map.Entry<UUID, Long> entry : cartDto.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Long requestedQuantity = entry.getValue();

            validateQuantity(productId, requestedQuantity);

            WarehouseItem item = warehouseRepository.findByProductId(productId)
                    .orElseThrow(() -> new ProductInShoppingCartLowQuantityInWarehouse(
                            "Товар не найден на складе: " + productId));

            if (item.getQuantity() < requestedQuantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(
                        String.format("Недостаточно товара на складе. Доступно: %d", item.getQuantity()));
            }

            totalWeight += item.getWeight() * requestedQuantity;
            totalVolume += item.getVolume() * requestedQuantity;
            if (Boolean.TRUE.equals(item.getFragile())) {
                hasFragile = true;
            }
        }

        return BookedProductsDto.builder()
                .deliveryWeight(totalWeight)
                .deliveryVolume(totalVolume)
                .fragile(hasFragile)
                .build();
    }

    @Transactional
    public void reserveProducts(ShoppingCartDto cartDto) {
        validateShoppingCart(cartDto);

        for (Map.Entry<UUID, Long> entry : cartDto.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            WarehouseItem item = warehouseRepository.findByProductId(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException(
                            "Товар не найден: " + productId));

            if (item.getQuantity() < quantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouse(
                        String.format("Недостаточно товара на складе. Доступно: %d, Запрошено: %d",
                                item.getQuantity(), quantity));
            }

            item.setQuantity(item.getQuantity() - quantity);
        }
    }

    @Transactional(readOnly = true)
    public boolean areProductsAvailable(ShoppingCartDto cartDto) {
        try {
            checkProductQuantityEnoughForShoppingCart(cartDto);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void validateNewProductRequest(NewProductInWarehouseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("ID продукта обязателен");
        }
        if (request.getDimension() == null) {
            throw new IllegalArgumentException("Размеры продукта обязательны");
        }
        if (request.getWeight() == null || request.getWeight() <= 0) {
            throw new IllegalArgumentException("Вес продукта должен быть положительным");
        }
    }

    private void validateAddProductRequest(AddProductToWarehouseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Запрос не может быть пустым");
        }
        if (request.getProductId() == null) {
            throw new IllegalArgumentException("ID продукта обязателен");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
    }

    private void validateShoppingCart(ShoppingCartDto cartDto) {
        if (cartDto == null) {
            throw new IllegalArgumentException("Корзина не может быть пустой");
        }
        if (cartDto.getProducts() == null || cartDto.getProducts().isEmpty()) {
            throw new NoProductsInShoppingCartException("Корзина пуста");
        }
    }

    private void validateQuantity(UUID productId, Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    String.format("Количество должно быть положительным для товара: %s", productId));
        }
    }
}