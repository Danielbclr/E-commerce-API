package com.danbramos.e_commerce_api.user;

import com.danbramos.e_commerce_api.role.Role;
import com.danbramos.e_commerce_api.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Import Slf4j for logging
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for managing {@link User} entities and handling user authentication details.
 * Implements both {@link UserService} for general user operations and {@link UserDetailsService}
 * for Spring Security integration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates and saves a new user based on the provided registration data.
     * Encodes the password and assigns the default 'ROLE_USER'.
     * Ensures the 'ROLE_USER' exists, creating it if necessary.
     * This operation is transactional.
     *
     * @param userDto The {@link UserRegistrationDTO} containing the new user's details.
     */
    @Override
    @Transactional
    public void saveUser(UserRegistrationDTO userDto) {
        log.info("Attempting to save new user with email: {}", userDto.email());
        User user = new User();
        user.setName(userDto.name()); // Simpler mapping
        user.setEmail(userDto.email());
        user.setPassword(passwordEncoder.encode(userDto.password()));

        Role userRole = checkAndCreateRole("ROLE_USER");
        user.setRoles(Arrays.asList(userRole));

        userRepository.save(user);
        log.info("Successfully saved new user with email: {}", userDto.email());
    }

    /**
     * Finds a user entity by their email address.
     * This operation is transactional and read-only.
     *
     * @param email The email address to search for.
     * @return The {@link User} entity if found, otherwise {@code null}.
     */
    @Override
    @Transactional(readOnly = true) // Read-only transaction for fetching data.
    public User findUserByEmail(String email) {
        log.debug("Attempting to find user by email: {}", email);
        return userRepository.findByEmail(email);
    }

    /**
     * Retrieves a list of all registered users, mapped to {@link UserDTO}.
     * This operation is transactional and read-only.
     * Note: Password information is excluded in the mapping.
     *
     * @return A list of {@link UserDTO} objects representing all users.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findAllUsers() {
        log.debug("Fetching all users");
        List<User> users = userRepository.findAll();
        List<UserDTO> userDtos = users.stream()
                .map(this::mapToUserDto)
                .collect(Collectors.toList());
        log.debug("Found {} users", userDtos.size());
        return userDtos;
    }

    /**
     * Locates the user based on the username (email in this case) for Spring Security authentication.
     * This is required by the {@link UserDetailsService} interface.
     * This operation is transactional and read-only.
     *
     * @param usernameOrEmail The email address of the user attempting to log in.
     * @return A {@link UserDetails} object containing user credentials and authorities.
     * @throws UsernameNotFoundException if no user is found with the given email.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user by username (email): {}", usernameOrEmail);
        User user = userRepository.findByEmail(usernameOrEmail);
        if (user == null) {
            log.warn("User not found during authentication: {}", usernameOrEmail);
            throw new UsernameNotFoundException("Invalid username or password.");
        }
        Collection<? extends GrantedAuthority> authorities = mapRolesToAuthorities(user.getRoles());
        log.debug("User found: {}, Roles: {}", usernameOrEmail, authorities);

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(), // Provide the encoded password
                authorities);       // Provide the authorities (roles)
    }

    /**
     * Maps a {@link User} entity to a {@link UserDTO}.
     * Extracts first and last names from the combined name field and excludes the password.
     *
     * @param user The {@link User} entity to map.
     * @return The corresponding {@link UserDTO}.
     */
    private UserDTO mapToUserDto(User user) {
        return new UserDTO(user.getId(), user.getName(), user.getEmail());
    }

    /**
     * Converts a collection of {@link Role} entities into a collection of
     * {@link GrantedAuthority} objects required by Spring Security.
     *
     * @param roles The collection of {@link Role} entities associated with a user.
     * @return A collection of {@link SimpleGrantedAuthority} objects.
     */
    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Collection<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName())) // Create authority from role name
                .collect(Collectors.toList());
    }

    /**
     * Checks if a role with the given name exists. If not, creates and saves it.
     * This helps ensure necessary roles are present in the database.
     * Note: This method implicitly starts a new transaction if called outside an existing one,
     * or participates in the existing transaction if called from within one (like from saveUser).
     *
     * @param roleName The name of the role to check or create (e.g., "ROLE_USER").
     * @return The existing or newly created {@link Role} entity.
     */
    private Role checkAndCreateRole(String roleName) {
        log.debug("Checking for role: {}", roleName);
        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            log.info("Role '{}' not found, creating it.", roleName);
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
            log.info("Role '{}' created successfully.", roleName);
        } else {
            log.debug("Role '{}' found.", roleName);
        }
        return role;
    }

}