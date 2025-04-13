package com.danbramos.e_commerce_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the application's web security settings using Spring Security.
 * Defines authorization rules for different HTTP endpoints, enables method-level security,
 * and sets up basic authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {


    private static final String API_BASE_PATH = "/api/v1";

    /**
     * Defines the main security filter chain bean that applies security rules to HTTP requests.
     * Configures CSRF protection, authorization rules for specific paths, session management,
     * and the authentication mechanism (HTTP Basic).
     *
     * @param http The {@link HttpSecurity} object to configure.
     * @return The configured {@link SecurityFilterChain} bean.
     * @throws Exception if an error occurs during configuration.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (Cross-Site Request Forgery) protection.
                // Suitable for stateless APIs where browsers don't manage sessions with cookies.
                .csrf(AbstractHttpConfigurer::disable)

                // Configure authorization rules for HTTP requests.
                .authorizeHttpRequests(authz -> authz
                        // --- Public Access Rules ---
                        .requestMatchers(HttpMethod.POST, API_BASE_PATH + "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, API_BASE_PATH + "/products", API_BASE_PATH + "/products/**").permitAll()

                        // --- Swagger / OpenAPI Documentation Access ---
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // --- Admin Restricted Rules ---
                        .requestMatchers(HttpMethod.POST, API_BASE_PATH + "/products").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, API_BASE_PATH + "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, API_BASE_PATH + "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, API_BASE_PATH + "/users").hasRole("ADMIN")

                        // --- General Authenticated Access Rule ---
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}