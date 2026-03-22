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
import ru.yandex.practicum.entity.Product;
import ru.yandex.practicum.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
public class ShoppingStoreController implements ShoppingStoreClient {

    private final ProductRepository productRepository;

    @Autowired
    public ShoppingStoreController(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
            return productRepository.findByProductCategory(productCategory, pageable)
                    .map(this::convertToDto);
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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Товар не найден"
                ));
        return convertToDto(product);
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

        Product product = convertToEntity(productDto);
        if (product.getProductState() == null) {
            product.setProductState(ProductState.ACTIVE);
        }

        try {
            Product savedProduct = productRepository.save(product);
            return convertToDto(savedProduct);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при сохранении товара"
            );
        }
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

        Product existingProduct = productRepository.findById(productDto.getProductId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Товар не найден"
                ));

        updateEntityFromDto(existingProduct, productDto);

        try {
            Product updatedProduct = productRepository.save(existingProduct);
            return convertToDto(updatedProduct);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при обновлении товара"
            );
        }
    }

    @Override
    @PostMapping("/removeProductFromStore")
    public Boolean removeProductFromStore(@RequestBody UUID productId) {
        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Товар не найден"
                    ));

            if (product.getProductState() == ProductState.DEACTIVATE) {
                return false;
            }

            product.setProductState(ProductState.DEACTIVATE);
            productRepository.save(product);
            return true;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при удалении товара"
            );
        }
    }

    @Override
    @PostMapping("/quantityState")
    public Boolean setProductQuantityState(
            @RequestParam("productId") UUID productId,
            @RequestParam("quantityState") QuantityState quantityState) {

        try {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Товар не найден"
                    ));

            product.setQuantityState(quantityState);
            productRepository.save(product);
            return true;
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ошибка при изменении состояния количества"
            );
        }
    }

    private ProductDto convertToDto(Product product) {
        return ProductDto.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .description(product.getDescription())
                .imageSrc(product.getImageSrc())
                .quantityState(product.getQuantityState())
                .productState(product.getProductState())
                .productCategory(product.getProductCategory())
                .price(product.getPrice())
                .build();
    }

    private Product convertToEntity(ProductDto dto) {
        return Product.builder()
                .productName(dto.getProductName())
                .description(dto.getDescription())
                .imageSrc(dto.getImageSrc())
                .quantityState(dto.getQuantityState())
                .productState(dto.getProductState())
                .productCategory(dto.getProductCategory())
                .price(dto.getPrice())
                .build();
    }

    private void updateEntityFromDto(Product product, ProductDto dto) {
        if (dto.getProductName() != null) product.setProductName(dto.getProductName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getImageSrc() != null) product.setImageSrc(dto.getImageSrc());
        if (dto.getQuantityState() != null) product.setQuantityState(dto.getQuantityState());
        if (dto.getProductState() != null) product.setProductState(dto.getProductState());
        if (dto.getProductCategory() != null) product.setProductCategory(dto.getProductCategory());
        if (dto.getPrice() > 0) product.setPrice(dto.getPrice());
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