package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippedToDeliveryRequest {

    @NotNull(message = "ID заказа обязателен")
    private UUID orderId;

    @NotNull(message = "ID доставки обязателен")
    private UUID deliveryId;
}