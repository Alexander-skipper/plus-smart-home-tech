package ru.yandex.practicum.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.entity.Product;
import ru.yandex.practicum.repository.ProductRepository;

import java.util.UUID;

@Service
public class ShoppingStoreService {

    private final ProductRepository productRepository;

    @Autowired
    public ShoppingStoreService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> getProducts(ProductCategory category, Pageable pageable) {
        return productRepository.findByProductCategory(category, pageable)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Товар не найден"
                ));
        return convertToDto(product);
    }

    @Transactional
    public ProductDto createNewProduct(ProductDto productDto) {
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

    @Transactional
    public ProductDto updateProduct(ProductDto productDto) {
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

    @Transactional
    public Boolean removeProductFromStore(UUID productId) {
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

    @Transactional
    public Boolean setProductQuantityState(UUID productId, QuantityState quantityState) {
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
}