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
public class ProductReturnRequest {

    private UUID orderId;

    @NotNull(message = "Список возвращаемых товаров обязателен")
    private Map<UUID, Long> products;
}