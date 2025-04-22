package com.danbramos.e_commerce_api.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link OrderItem} entity.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

}