package com.danbramos.e_commerce_api.user;

import com.danbramos.e_commerce_api.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByEmail(String email);

    @Modifying // Important for delete operations
    @Query("DELETE FROM User u WHERE u.id IN :ids")
    void deleteUsersWithIds(@Param("ids") List<Long> ids);

    // Optional: Find users not having the admin role (useful for logging/confirmation)
    @Query("SELECT u FROM User u WHERE :adminRole NOT MEMBER OF u.roles")
    List<User> findUsersByRolesNotContaining(@Param("adminRole") Role adminRole);
}