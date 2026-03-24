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
public class ShoppingCartDto {

    @NotNull(message = "ID корзины покупок обязательно")
    private UUID shoppingCartId;

    @NotNull(message = "Список продуктов обязателен")
    private Map<UUID, Long> products;
}