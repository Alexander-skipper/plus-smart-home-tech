package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "warehouse_items", schema = "warehouse_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID productId;

    @Column(nullable = false)
    @Builder.Default
    private Long quantity = 0L;

    @Column(nullable = false)
    private Double width;

    @Column(nullable = false)
    private Double height;

    @Column(nullable = false)
    private Double depth;

    @Column(nullable = false)
    private Double weight;

    private Boolean fragile;

    public Double getVolume() {
        return width * height * depth;
    }

    public boolean isAvailable(Long requestedQuantity) {
        return this.quantity >= requestedQuantity;
    }

    public void reserve(Long quantityToReserve) {
        if (!isAvailable(quantityToReserve)) {
            throw new IllegalStateException("Недостаточно товара на складе");
        }
        this.quantity -= quantityToReserve;
    }

    public void addStock(Long quantityToAdd) {
        if (quantityToAdd < 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        this.quantity += quantityToAdd;
    }

    public boolean isFragile() {
        return Boolean.TRUE.equals(fragile);
    }
}