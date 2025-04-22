package com.danbramos.e_commerce_api.product;

import com.danbramos.e_commerce_api.exception.InsufficientStockException;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing products.
 * Defines the contract for product-related operations.
 */
public interface ProductService {

    /**
     * Retrieves all products.
     *
     * @return A list of all products.
     */
    List<Product> findAllProducts();

    /**
     * Finds a product by its ID.
     *
     * @param id The ID of the product to find.
     * @return An Optional containing the product if found, or empty otherwise.
     */
    Optional<Product> findProductById(Long id);

    /**
     * Saves a new product or updates an existing one.
     *
     * @param product The product entity to save.
     * @return The saved product entity (potentially with generated ID).
     */
    Product saveProduct(Product product);

    /**
     * Deletes a product by its ID.
     *
     * @param id The ID of the product to delete.
     */
    void deleteProduct(Long id) throws Exception;

    void verifyStockAvailability(Product product, int requestedQuantity) throws InsufficientStockException;

}