package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.entity.AddressEmbeddable;
import ru.yandex.practicum.entity.Delivery;
import ru.yandex.practicum.exceptions.NoDeliveryFoundException;
import ru.yandex.practicum.repository.DeliveryRepository;

import java.util.UUID;

@Service
public class DeliveryService {

    private static final double BASE_COST = 5.0;
    private static final String ADDRESS_1 = "ADDRESS_1";
    private static final String ADDRESS_2 = "ADDRESS_2";

    private final DeliveryRepository deliveryRepository;
    private final WarehouseClient warehouseClient;
    private final OrderClient orderClient;

    @Autowired
    public DeliveryService(DeliveryRepository deliveryRepository,
                           WarehouseClient warehouseClient,
                           OrderClient orderClient) {
        this.deliveryRepository = deliveryRepository;
        this.warehouseClient = warehouseClient;
        this.orderClient = orderClient;
    }

    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        Delivery delivery = Delivery.builder()
                .fromAddress(convertToEmbeddable(deliveryDto.getFromAddress()))
                .toAddress(convertToEmbeddable(deliveryDto.getToAddress()))
                .orderId(deliveryDto.getOrderId())
                .deliveryState(DeliveryState.CREATED)
                .build();

        Delivery savedDelivery = deliveryRepository.save(delivery);

        return DeliveryDto.builder()
                .deliveryId(savedDelivery.getDeliveryId())
                .fromAddress(deliveryDto.getFromAddress())
                .toAddress(deliveryDto.getToAddress())
                .orderId(savedDelivery.getOrderId())
                .deliveryState(savedDelivery.getDeliveryState())
                .build();
    }

    @Transactional
    public void deliveryPicked(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);
        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);
    }

    @Transactional
    public void deliverySuccessful(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);
        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        orderClient.delivery(orderId);
    }

    @Transactional
    public void deliveryFailed(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);
        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        orderClient.deliveryFailed(orderId);
    }

    @Transactional(readOnly = true)
    public Double deliveryCost(UUID orderId) {
        Delivery delivery = getDeliveryByOrderIdOrThrow(orderId);

        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();

        AddressDto deliveryAddress = convertToAddressDto(delivery.getToAddress());

        double cost = BASE_COST;

        if (ADDRESS_2.equals(warehouseAddress.getStreet())) {
            cost = cost + (BASE_COST * 2);
        } else {
            cost = cost + (BASE_COST * 1);
        }

        OrderDto order = orderClient.getOrder(orderId);

        if (order != null && Boolean.TRUE.equals(order.getFragile())) {
            cost = cost + (cost * 0.2);
        }

        if (order != null && order.getDeliveryWeight() != null) {
            cost = cost + (order.getDeliveryWeight() * 0.3);
        }

        if (order != null && order.getDeliveryVolume() != null) {
            cost = cost + (order.getDeliveryVolume() * 0.2);
        }

        if (deliveryAddress != null && warehouseAddress != null &&
                deliveryAddress.getStreet() != null &&
                !deliveryAddress.getStreet().equals(warehouseAddress.getStreet())) {
            cost = cost + (cost * 0.2);
        }

        return cost;
    }

    private Delivery getDeliveryByOrderIdOrThrow(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException("Доставка не найдена для заказа: " + orderId));
    }

    private AddressEmbeddable convertToEmbeddable(AddressDto dto) {
        if (dto == null) return null;
        return AddressEmbeddable.builder()
                .country(dto.getCountry())
                .city(dto.getCity())
                .street(dto.getStreet())
                .house(dto.getHouse())
                .flat(dto.getFlat())
                .build();
    }

    private AddressDto convertToAddressDto(AddressEmbeddable embeddable) {
        if (embeddable == null) return null;
        return new AddressDto(
                embeddable.getCountry(),
                embeddable.getCity(),
                embeddable.getStreet(),
                embeddable.getHouse(),
                embeddable.getFlat()
        );
    }
}