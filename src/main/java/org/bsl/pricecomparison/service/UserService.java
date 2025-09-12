package org.bsl.pricecomparison.service;

import org.bsl.pricecomparison.dto.UserDTO;
import org.bsl.pricecomparison.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    User updateUser(String id, User user);
    void deleteUser(String id);
    Page<UserDTO> filterUsers(String username, String address, String phone, String email, String role, Pageable pageable);
}