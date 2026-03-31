package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssemblyProductsForOrderRequest {

    @NotNull(message = "ID заказа обязателен")
    private UUID orderId;

    @NotNull(message = "Список товаров обязателен")
    private Map<UUID, Long> products;
}