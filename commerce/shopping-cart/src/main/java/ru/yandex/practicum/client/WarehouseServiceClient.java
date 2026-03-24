package ru.yandex.practicum.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.BookedProductsDto;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.exceptions.ProductInShoppingCartLowQuantityInWarehouse;

@Component
public class WarehouseServiceClient {

    private static final Logger log = LoggerFactory.getLogger(WarehouseServiceClient.class);

    private final WarehouseClient warehouseClient;

    @Autowired
    public WarehouseServiceClient(WarehouseClient warehouseClient) {
        this.warehouseClient = warehouseClient;
    }

    @CircuitBreaker(name = "warehouse", fallbackMethod = "checkQuantityFallback")
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cartDto) {
        log.info("Проверка наличия товаров для корзины: {}", cartDto.getShoppingCartId());
        return warehouseClient.checkProductQuantityEnoughForShoppingCart(cartDto);
    }

    private BookedProductsDto checkQuantityFallback(ShoppingCartDto cartDto, Exception ex) {
        log.error("Сервис склада недоступен: {}", ex.getMessage());
        throw new ProductInShoppingCartLowQuantityInWarehouse(
                "Warehouse service is unavailable",
                "Сервис склада временно недоступен");
    }
}