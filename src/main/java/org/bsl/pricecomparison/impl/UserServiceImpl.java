package org.bsl.pricecomparison.impl;

import org.bsl.pricecomparison.dto.UserDTO;
import org.bsl.pricecomparison.model.User;
import org.bsl.pricecomparison.repository.UserRepository;
import org.bsl.pricecomparison.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User updateUser(String id, User user) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        existingUser.setUsername(user.getUsername());
        existingUser.setPassword(user.getPassword());
        existingUser.setAddress(user.getAddress());
        existingUser.setPhone(user.getPhone());
        existingUser.setEmail(user.getEmail());
        existingUser.setRole(user.getRole());
        existingUser.setProfileImageUrl(user.getProfileImageUrl());
        return userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(String id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        userRepository.delete(existingUser);
    }

    @Override
    public Page<UserDTO> filterUsers(
            String username,
            String address,
            String phone,
            String email,
            String role,
            Pageable pageable
    ) {
        Page<User> userPage = userRepository.filterUsers(
                username,
                address,
                phone,
                email,
                role,
                pageable
        );

        return userPage.map(user -> {
            UserDTO dto = new UserDTO();
            dto.setId(Objects.toString(user.getId(), ""));
            dto.setUsername(Objects.toString(user.getUsername(), ""));
            dto.setAddress(Objects.toString(user.getAddress(), ""));
            dto.setPhone(Objects.toString(user.getPhone(), ""));
            dto.setEmail(Objects.toString(user.getEmail(), ""));
            dto.setRole(Objects.toString(user.getRole(), ""));
            dto.setProfileImageUrl(Objects.toString(user.getProfileImageUrl(), ""));
            return dto;
        });
    }
}