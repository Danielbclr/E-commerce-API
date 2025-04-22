package com.danbramos.e_commerce_api.product;

import com.danbramos.e_commerce_api.exception.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for managing {@link Product} entities.
 * Provides business logic for product-related operations, interacting with the {@link ProductRepository}.
 * Uses Spring's transaction management via {@link Transactional}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * Retrieves all products from the repository.
     * Marked as read-only transaction as it doesn't modify data.
     *
     * @return A list of all {@link Product} entities. Returns an empty list if none are found.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        log.debug("Fetching all products"); // Added debug logging
        return productRepository.findAll();
    }

    /**
     * Finds a product by its unique identifier.
     * Marked as read-only transaction.
     *
     * @param id The ID of the product to find. Must not be null.
     * @return An {@link Optional} containing the found {@link Product}, or {@link Optional#empty()} if not found.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findProductById(Long id) {
        log.debug("Attempting to find product by ID: {}", id); // Added debug logging
        return productRepository.findById(id);
    }

    /**
     * Saves (creates or updates) a product entity in the repository.
     * This operation is transactional, ensuring atomicity.
     *
     * @param product The {@link Product} entity to save. Must not be null.
     * @return The saved {@link Product} entity, potentially with updated state (like a generated ID).
     */
    @Override
    @Transactional
    public Product saveProduct(Product product) {
        log.info("Saving product: {}", product.getName()); // Added info logging
        // Add any business logic/validation before saving if needed
        return productRepository.save(product);
    }

    /**
     * Deletes a product entity by its unique identifier.
     * This operation is transactional.
     *
     * @param id The ID of the product to delete. Must not be null.
     * @throws org.springframework.dao.EmptyResultDataAccessException if no product with the given ID exists (behavior depends on JPA provider).
     */
    @Override
    @Transactional
    public void deleteProduct(Long id) throws Exception {
        log.warn("Attempting to delete product with ID: {}", id); // Added warn logging for delete operations
         if (!productRepository.existsById(id)) {
             log.error("Attempted to delete non-existent product with ID: {}", id);
             throw new Exception("Product with id " + id + " not found."); // Or handle appropriately
         }
        productRepository.deleteById(id);
        log.info("Successfully deleted product with ID: {}", id);
    }

    @Override
    public void verifyStockAvailability(Product product, int requestedQuantity) throws InsufficientStockException {
        if (product.getStockQuantity() < requestedQuantity) {
            log.warn("Insufficient stock for Product ID: {}. Requested: {}, Available: {}",
                    product.getId(), requestedQuantity, product.getStockQuantity());
            throw new InsufficientStockException(
                    "Insufficient stock for product: " + product.getName() +
                            ". Available: " + product.getStockQuantity() +
                            ", Requested: " + requestedQuantity
            );

        }
    }
}