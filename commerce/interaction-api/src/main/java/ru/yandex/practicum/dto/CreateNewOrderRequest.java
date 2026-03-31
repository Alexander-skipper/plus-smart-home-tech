package ru.yandex.practicum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewOrderRequest {

    @NotNull(message = "Корзина обязательна")
    private ShoppingCartDto shoppingCart;

    @NotNull(message = "Адрес доставки обязателен")
    private AddressDto deliveryAddress;
}