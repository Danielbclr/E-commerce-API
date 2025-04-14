package com.danbramos.e_commerce_api.user;

import org.springframework.stereotype.Service;

import java.util.List;

public interface UserService {
    void saveUser(UserRegistrationDTO userDto);
    User findUserByEmail(String email);
    List<UserDTO> findAllUsers();
    void deleteUserById(Long id);
    long deleteNonAdminUsers();
}