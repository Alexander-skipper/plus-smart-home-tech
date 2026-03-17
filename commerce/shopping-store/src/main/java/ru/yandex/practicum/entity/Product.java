package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.yandex.practicum.dto.ProductCategory;
import ru.yandex.practicum.dto.ProductState;
import ru.yandex.practicum.dto.QuantityState;

import java.util.UUID;

@Entity
@Table(name = "products", schema = "shopping_store_schema")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false, length = 1000)
    private String description;

    private String imageSrc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuantityState quantityState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductState productState;

    @Enumerated(EnumType.STRING)
    private ProductCategory productCategory;

    @Column(nullable = false)
    private Double price;

    @Builder
    private Product(String productName, String description, String imageSrc,
                    QuantityState quantityState, ProductState productState,
                    ProductCategory productCategory, Double price) {

        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Название продукта обязательно");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Описание продукта обязательно");
        }
        if (price == null) {
            throw new IllegalArgumentException("Цена продукта обязательна");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Цена не может быть отрицательной");
        }
        if (quantityState == null) {
            throw new IllegalArgumentException("Состояние количества обязательно");
        }
        if (productState == null) {
            throw new IllegalArgumentException("Состояние продукта обязательно");
        }

        this.productName = productName;
        this.description = description;
        this.imageSrc = imageSrc;
        this.quantityState = quantityState;
        this.productState = productState;
        this.productCategory = productCategory;
        this.price = price;
    }
}