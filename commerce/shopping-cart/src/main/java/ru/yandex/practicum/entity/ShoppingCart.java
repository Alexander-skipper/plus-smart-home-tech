package ru.yandex.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Entity
@Table(name = "shopping_carts", schema = "shopping_cart_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShoppingCart {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID shoppingCartId;

    @Column(nullable = false, unique = true)
    private String username;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_products", schema = "shopping_cart_schema",
            joinColumns = @JoinColumn(name = "cart_id"))
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    @Builder.Default
    private Map<UUID, Long> products = new ConcurrentHashMap<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version
    private Long version;

    public void addProduct(UUID productId, Long quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Количество должно быть положительным");
        }
        products.merge(productId, quantity, Long::sum);
    }

    public void removeProduct(UUID productId) {
        products.remove(productId);
    }

    public void updateProductQuantity(UUID productId, Long newQuantity) {
        if (newQuantity <= 0) {
            removeProduct(productId);
        } else {
            products.put(productId, newQuantity);
        }
    }

    public void clear() {
        products.clear();
    }

    public Long getTotalItems() {
        return products.values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    public boolean isEmpty() {
        return products.isEmpty();
    }

    public void deactivate() {
        this.active = false;
    }
}