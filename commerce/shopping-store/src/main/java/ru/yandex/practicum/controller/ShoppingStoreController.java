package ru.yandex.practicum.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import ru.yandex.practicum.client.ShoppingStoreClient;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.service.ShoppingStoreService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
public class ShoppingStoreController implements ShoppingStoreClient {

    private final ShoppingStoreService shoppingStoreService;

    @Autowired
    public ShoppingStoreController(ShoppingStoreService shoppingStoreService) {
        this.shoppingStoreService = shoppingStoreService;
    }

    @Override
    @GetMapping
    public Page<ProductDto> getProducts(
            @RequestParam(value = "category", required = true) String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "productName,asc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        try {
            ProductCategory productCategory = ProductCategory.valueOf(category);
            return shoppingStoreService.getProducts(productCategory, pageable);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Неверная категория товара: " + category
            );
        }
    }

    @Override
    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable("productId") UUID productId) {
        return shoppingStoreService.getProduct(productId);
    }

    @Override
    @PutMapping
    public ProductDto createNewProduct(@Valid @RequestBody ProductDto productDto) {
        if (productDto.getProductId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID товара не должен быть указан при создании"
            );
        }
        return shoppingStoreService.createNewProduct(productDto);
    }

    @Override
    @PostMapping
    public ProductDto updateProduct(@Valid @RequestBody ProductDto productDto) {
        if (productDto.getProductId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID товара обязательно для обновления"
            );
        }
        return shoppingStoreService.updateProduct(productDto);
    }

    @Override
    @PostMapping("/removeProductFromStore")
    public Boolean removeProductFromStore(@RequestBody UUID productId) {
        return shoppingStoreService.removeProductFromStore(productId);
    }

    @Override
    @PostMapping("/quantityState")
    public Boolean setProductQuantityState(
            @RequestParam("productId") UUID productId,
            @RequestParam("quantityState") QuantityState quantityState) {
        return shoppingStoreService.setProductQuantityState(productId, quantityState);
    }

    private Sort parseSort(String[] sortParams) {
        if (sortParams == null || sortParams.length == 0) {
            return Sort.by(Sort.Direction.ASC, "productName");
        }

        List<Sort.Order> orders = new ArrayList<>();

        if (sortParams.length > 1 && (sortParams[1].equalsIgnoreCase("ASC")
                || sortParams[1].equalsIgnoreCase("DESC"))) {
            String property = sortParams[0].trim();
            Sort.Direction direction = sortParams[1].equalsIgnoreCase("DESC")
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, property));
        } else {
            for (String param : sortParams) {
                String[] parts = param.split(",");
                String property = parts[0].trim();
                Sort.Direction direction = Sort.Direction.ASC;
                if (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())) {
                    direction = Sort.Direction.DESC;
                }
                orders.add(new Sort.Order(direction, property));
            }
        }

        return Sort.by(orders);
    }
}