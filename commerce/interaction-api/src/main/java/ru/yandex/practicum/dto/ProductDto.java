package ru.yandex.practicum.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID productId;

    @NotBlank(message = "Название продукта обязательно")
    private String productName;

    @NotBlank(message = "Описание обязательно")
    private String description;

    private String imageSrc;

    @NotNull(message = "Состояние количества обязательно")
    private QuantityState quantityState;

    @NotNull(message = "Состояние продукта обязательно")
    private ProductState productState;

    private ProductCategory productCategory;

    @Min(value = 1, message = "Цена должна быть не меньше 1")
    private double price;
}