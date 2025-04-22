package com.danbramos.e_commerce_api.order;

import com.danbramos.e_commerce_api.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Order} entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds all orders placed by a specific user, ordered by order date descending.
     *
     * @param user The user whose orders are to be retrieved.
     * @return A list of orders for the given user.
     */
    List<Order> findByUserOrderByOrderDateDesc(User user);

    /**
     * Finds a specific order by its ID and the user who placed it.
     * Useful for ensuring a user can only access their own orders.
     *
     * @param id   The ID of the order.
     * @param user The user who should own the order.
     * @return An Optional containing the order if found and owned by the user, otherwise empty.
     */
    Optional<Order> findByIdAndUser(Long id, User user);

}