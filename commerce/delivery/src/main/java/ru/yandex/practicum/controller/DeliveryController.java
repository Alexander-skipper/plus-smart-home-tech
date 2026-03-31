package ru.yandex.practicum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.DeliveryClient;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.service.DeliveryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery")
public class DeliveryController implements DeliveryClient {

    private final DeliveryService deliveryService;

    @Autowired
    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Override
    @PutMapping
    public DeliveryDto planDelivery(@RequestBody DeliveryDto deliveryDto) {
        return deliveryService.planDelivery(deliveryDto);
    }

    @Override
    @PostMapping("/successful")
    public void deliverySuccessful(@RequestBody UUID orderId) {
        deliveryService.deliverySuccessful(orderId);
    }

    @Override
    @PostMapping("/picked")
    public void deliveryPicked(@RequestBody UUID orderId) {
        deliveryService.deliveryPicked(orderId);
    }

    @Override
    @PostMapping("/failed")
    public void deliveryFailed(@RequestBody UUID orderId) {
        deliveryService.deliveryFailed(orderId);
    }

    @Override
    @PostMapping("/cost/{orderId}")
    public Double deliveryCost(@PathVariable("orderId") UUID orderId) {
        return deliveryService.deliveryCost(orderId);
    }
}