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
import ru.yandex.practicum.dto.ProductCategory;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.ProductState;
import ru.yandex.practicum.dto.SetProductQuantityStateRequest;
import ru.yandex.practicum.entity.Product;
import ru.yandex.practicum.repository.ProductRepository;

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
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "productName,asc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));

        if (category == null || category.isEmpty()) {
            return productRepository.findAll(pageable)
                    .map(this::convertToDto);
        }

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
        product.setProductState(ProductState.ACTIVE);

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
            @Valid @RequestBody SetProductQuantityStateRequest request) {

        try {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Товар не найден"
                    ));

            product.setQuantityState(request.getQuantityState());
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
        if (dto.getProductCategory() != null) product.setProductCategory(dto.getProductCategory());
        if (dto.getPrice() > 0) product.setPrice(dto.getPrice());
    }

    private Sort.Order[] parseSort(String[] sortParams) {
        if (sortParams == null || sortParams.length == 0) {
            return new Sort.Order[]{Sort.Order.asc("productName")};
        }

        return java.util.Arrays.stream(sortParams)
                .map(param -> {
                    String[] parts = param.split(",");
                    String property = parts[0];
                    Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                            ? Sort.Direction.DESC : Sort.Direction.ASC;
                    return new Sort.Order(direction, property);
                })
                .toArray(Sort.Order[]::new);
    }
}