package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.entity.WarehouseItem;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<WarehouseItem, UUID> {
    Optional<WarehouseItem> findByProductId(UUID productId);
    boolean existsByProductId(UUID productId);
}