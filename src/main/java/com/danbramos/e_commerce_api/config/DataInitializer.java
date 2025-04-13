package com.danbramos.e_commerce_api.config;

import com.danbramos.e_commerce_api.role.Role;
import com.danbramos.e_commerce_api.role.RoleRepository;
import com.danbramos.e_commerce_api.user.User;
import com.danbramos.e_commerce_api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // Import @Value
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Initializes essential data upon application startup.
 * This component ensures that necessary roles (ADMIN, USER) and a default
 * administrator user exist in the database using configuration properties.
 * It implements {@link CommandLineRunner} to execute its logic after the
 * Spring application context is loaded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // Inject properties from application.properties
    @Value("${admin.init.email}")
    private String adminEmail;

    @Value("${admin.init.password}")
    private String adminPassword;

    @Value("${admin.init.firstName}")
    private String adminFirstName;

    @Value("${admin.init.lastName}")
    private String adminLastName;

    // Role names can remain constants if they are fundamental to the code logic
    private static final String ADMIN_ROLE_NAME = "ROLE_ADMIN";
    private static final String USER_ROLE_NAME = "ROLE_USER";

    /**
     * Executes the data initialization logic using configured properties.
     * Checks for the existence of ADMIN and USER roles, creating them if necessary.
     * Checks for the existence of the default admin user (based on configured email),
     * creating it with configured details if necessary.
     * This method is automatically called by Spring Boot upon application startup.
     *
     * @param args Incoming command line arguments (not used in this implementation).
     * @throws Exception if any error occurs during the initialization process.
     */
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Checking for initial roles and admin user...");

        Role adminRole = checkAndCreateRole(ADMIN_ROLE_NAME);
        checkAndCreateRole(USER_ROLE_NAME);

        if (userRepository.findByEmail(adminEmail) == null) {
            log.info("Admin user not found, creating initial admin user...");
            User adminUser = new User();
            adminUser.setName(adminFirstName + " " + adminLastName);
            adminUser.setEmail(adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRoles(Collections.singletonList(adminRole));

            userRepository.save(adminUser);
            log.info("Initial admin user created successfully with email: {}", adminEmail);
        } else {
            log.info("Admin user already exists.");
        }
    }

    /**
     * Checks if a role with the specified name exists in the repository.
     * If the role does not exist, it creates and saves a new role with that name.
     *
     * @param roleName The name of the role to check or create (e.g., "ROLE_ADMIN").
     * @return The existing or newly created {@link Role} entity.
     */
    private Role checkAndCreateRole(String roleName) {
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            log.info("Role {} not found, creating...", roleName);
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
            log.info("Role {} created.", roleName);
        }
        return role;
    }
}
    