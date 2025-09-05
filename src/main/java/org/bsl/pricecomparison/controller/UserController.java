package org.bsl.pricecomparison.controller;

import org.bsl.pricecomparison.model.User;
import org.bsl.pricecomparison.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/add")
    public ResponseEntity<User> addUser(@RequestBody User user) {
        User savedUser = userService.saveUser(user);
        return ResponseEntity.ok(savedUser);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody User loginRequest) {
        Optional<User> userOpt = userService.findByEmail(loginRequest.getEmail());
        Map<String, Object> response = new HashMap<>();

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(loginRequest.getPassword())) {
                // Giả lập token (trong thực tế, sử dụng JWT hoặc OAuth)
                String token = "dummy-token-" + user.getId(); // Thay bằng logic tạo token thực tế
                response.put("token", token);
                response.put("user", user);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(401).body(response);
        }
        response.put("message", "User not found");
        return ResponseEntity.status(404).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> updateUser(
            @PathVariable String id,
            @RequestPart("user") User user,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {
        String profileImageUrl = null;
        if (profileImage != null && !profileImage.isEmpty()) {
            profileImageUrl = saveProfileImage(profileImage);
            user.setProfileImageUrl(profileImageUrl);
        }
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private String saveProfileImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String contentType = file.getContentType();
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IOException("Only JPEG, PNG, and GIF files are allowed");
        }
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);
        Files.createDirectories(path.getParent());
        file.transferTo(path);
        return "/uploads/" + fileName;
    }
}