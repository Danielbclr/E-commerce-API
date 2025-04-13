package com.danbramos.e_commerce_api.product;

import io.swagger.v3.oas.annotations.Operation; // Import OpenAPI annotations
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag; // Import Tag annotation
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Add logging
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // Import MediaType for POST/PUT
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For method-level security
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST controller for managing products in the e-commerce application.
 * Provides endpoints for creating, retrieving, updating, and deleting products.
 */
@RestController
@RequestMapping("${api-base-path}/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing products")
@Slf4j
public class ProductController {

    private final ProductService productService;

    /**
     * Retrieves a list of all available products.
     * This endpoint is publicly accessible.
     *
     * @return A ResponseEntity containing a list of {@link Product} objects and HTTP status 200 (OK).
     */
    @Operation(summary = "Get all products", description = "Retrieves a list of all products.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of products",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = List.class)))
    })
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        log.info("Request received to get all products");
        List<Product> products = productService.findAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * Retrieves a specific product by its unique ID.
     * This endpoint is publicly accessible.
     *
     * @param id The ID of the product to retrieve.
     * @return A ResponseEntity containing the {@link Product} if found (HTTP status 200 OK),
     * or HTTP status 404 (Not Found) if no product with the given ID exists.
     */
    @Operation(summary = "Get product by ID", description = "Retrieves a specific product using its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved product",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "404", description = "Product not found with the specified ID",
                    content = @Content) // No content for 404
    })
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(
            @Parameter(description = "ID of the product to be retrieved", required = true) @PathVariable Long id) {
        log.info("Request received to get product with ID: {}", id);
        return productService.findProductById(id)
                .map(product -> {
                    log.info("Found product with ID: {}", id);
                    return ResponseEntity.ok(product);
                })
                .orElseGet(() -> { // Use orElseGet for lazy evaluation
                    log.warn("Product not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Creates a new product.
     * Requires ADMIN role. The request body must contain valid product data.
     *
     * @param productDto The data transfer object containing the details of the product to create.
     * @return A ResponseEntity containing the newly created {@link Product} and HTTP status 201 (Created).
     * @throws ResponseStatusException with status 500 if an unexpected error occurs.
     */
    @Operation(summary = "Create a new product", description = "Adds a new product to the catalog. Requires ADMIN role.",
            security = @SecurityRequirement(name = "basicAuth")) // Reference security scheme defined elsewhere (e.g., @OpenAPIDefinition)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "400", description = "Invalid product data provided",
                    content = @Content), // Bad Request if validation fails
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges (ADMIN role required)", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during product creation", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE) // Specify consumes/produces
    @PreAuthorize("hasRole('ADMIN')") // Enforce ADMIN role at method level
    public ResponseEntity<Product> createProduct(
            @Parameter(description = "Product data to create", required = true,
                    schema = @Schema(implementation = ProductDTO.class))
            @Valid @RequestBody ProductDTO productDto) {
        log.info("Request received to create product: {}", productDto.name());
        try {
            // Map DTO to Entity - Consider using a mapping library like MapStruct
            Product newProduct = new Product();
            newProduct.setName(productDto.name());
            newProduct.setDescription(productDto.description());
            newProduct.setPrice(productDto.price());
            newProduct.setStockQuantity(productDto.stockQuantity());
            newProduct.setImageUrl(productDto.imageUrl());

            Product savedProduct = productService.saveProduct(newProduct);
            log.info("Product created successfully with ID: {}", savedProduct.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        } catch (Exception e) {
            log.error("Error creating product '{}': {}", productDto.name(), e.getMessage(), e);
            // Let Spring's default exception handling or a @ControllerAdvice handle this
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating product", e);
        }
    }

    /**
     * Updates an existing product identified by its ID.
     * Requires ADMIN role. The request body must contain valid product data for the update.
     *
     * @param id         The ID of the product to update.
     * @param productDto The data transfer object containing the updated product details.
     * @return A ResponseEntity containing the updated {@link Product} (HTTP status 200 OK),
     * or HTTP status 404 (Not Found) if no product with the given ID exists.
     * @throws ResponseStatusException with status 500 if an unexpected error occurs.
     */
    @Operation(summary = "Update an existing product", description = "Updates details of a product by its ID. Requires ADMIN role.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Product.class))),
            @ApiResponse(responseCode = "400", description = "Invalid product data provided", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges (ADMIN role required)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found with the specified ID", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during product update", content = @Content)
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(
            @Parameter(description = "ID of the product to be updated", required = true) @PathVariable Long id,
            @Parameter(description = "Updated product data", required = true,
                    schema = @Schema(implementation = ProductDTO.class))
            @Valid @RequestBody ProductDTO productDto) {
        log.info("Request received to update product with ID: {}", id);
        try {
            return productService.findProductById(id)
                    .map(existingProduct -> {
                        // Update fields from DTO - Consider a dedicated update method in the service
                        existingProduct.setName(productDto.name());
                        existingProduct.setDescription(productDto.description());
                        existingProduct.setPrice(productDto.price());
                        existingProduct.setStockQuantity(productDto.stockQuantity());
                        existingProduct.setImageUrl(productDto.imageUrl());

                        Product updatedProduct = productService.saveProduct(existingProduct);
                        log.info("Product updated successfully with ID: {}", id);
                        return ResponseEntity.ok(updatedProduct);
                    })
                    .orElseGet(() -> {
                        log.warn("Product not found with ID {} during update attempt", id);
                        return ResponseEntity.notFound().build();
                    });
        } catch (Exception e) {
            log.error("Error updating product with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating product", e);
        }
    }

    /**
     * Deletes a product identified by its ID.
     * Requires ADMIN role.
     *
     * @param id The ID of the product to delete.
     * @return A ResponseEntity with HTTP status 204 (No Content) if successful,
     * or HTTP status 404 (Not Found) if no product with the given ID exists.
     * @throws ResponseStatusException with status 500 if an unexpected error occurs.
     */
    @Operation(summary = "Delete a product", description = "Deletes a product by its ID. Requires ADMIN role.",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges (ADMIN role required)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Product not found with the specified ID", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error during product deletion", content = @Content)
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "ID of the product to be deleted", required = true) @PathVariable Long id) {
        log.info("Request received to delete product with ID: {}", id);
        // Check existence first to provide accurate 404
        if (productService.findProductById(id).isEmpty()) {
            log.warn("Product not found with ID {} during delete attempt", id);
            return ResponseEntity.notFound().build();
        }
        try {
            productService.deleteProduct(id);
            log.info("Product deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build(); // 204 No Content is standard for successful DELETE
        } catch (Exception e) {
            // Catch specific exceptions if possible (e.g., DataIntegrityViolationException)
            log.error("Error deleting product with ID {}: {}", id, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting product", e);
        }
    }
}