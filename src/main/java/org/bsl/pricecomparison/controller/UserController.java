package org.bsl.pricecomparison.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.bsl.pricecomparison.dto.ChangePasswordDTO;
import org.bsl.pricecomparison.dto.LoginDTO;
import org.bsl.pricecomparison.dto.UserDTO;
import org.bsl.pricecomparison.model.User;
import org.bsl.pricecomparison.request.UserRequest;
import org.bsl.pricecomparison.security.JwtUtil;
import org.bsl.pricecomparison.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper; // Add ObjectMapper for JSON deserialization
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String UPLOAD_DIR = "uploads/users/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB


    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Create a new user with optional profile image",
            description = "Create a new user with user data (form parameters) and an optional profile image using multipart/form-data.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = UserRequest.class)
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> addUser(@ModelAttribute UserRequest request) {
        try {
            // Log received data
            logger.info("Received user data: username={}, email={}, password={}, address={}, phone={}, role={}",
                    request.getUsername(), request.getEmail(), request.getPassword(),
                    request.getAddress(), request.getPhone(), request.getRole());
            logger.info("Received profileImage: {}", request.getProfileImage() != null ? request.getProfileImage().getOriginalFilename() : "null");

            // Validate required fields
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Username cannot be empty"));
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email cannot be empty"));
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Password cannot be empty"));
            }

            // Construct User object
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword());
            user.setAddress(request.getAddress());
            user.setPhone(request.getPhone());
            user.setRole(request.getRole());

            // Check for duplicate email
            if (userService.findByEmail(user.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "User with this email already exists"));
            }

            // Handle image upload
            String profileImageUrl = null;
            MultipartFile profileImage = request.getProfileImage();
            if (profileImage != null && !profileImage.isEmpty()) {
                profileImageUrl = saveProfileImage(profileImage);
                logger.info("Profile image saved at: {}", profileImageUrl);
                // Set image URL to user
                user.setProfileImageUrl(profileImageUrl);
            }

            // Set additional fields
            user.setId(UUID.randomUUID().toString());
            user.setCreatedAt(LocalDateTime.now());

            // Save user
            User savedUser = userService.saveUser(user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User created successfully");
            response.put("data", savedUser);
            if (profileImageUrl != null) {
                response.put("profileImageUrl", profileImageUrl);
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            logger.error("Error processing file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error processing file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error in addUser: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error: " + e.getMessage()));
        }
    }

    private String saveProfileImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Image file size exceeds limit of 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !Arrays.asList("image/jpeg", "image/png", "image/gif").contains(contentType)) {
            throw new IOException("Only JPEG, PNG, and GIF files are allowed");
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + fileName);

        Files.createDirectories(path.getParent());

        try {
            file.transferTo(path);
            logger.info("Saved file to: {}", path.toString());
        } catch (IOException e) {
            logger.error("Failed to save file: {}", e.getMessage(), e);
            throw new IOException("Failed to save profile image: " + file.getOriginalFilename(), e);
        }

        return "/uploads/users/" + fileName;
    }

    @GetMapping
    @Operation(
            summary = "Filter users",
            description = "Retrieve a paginated list of users filtered by the provided criteria."
    )
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "") String username,
            @RequestParam(required = false, defaultValue = "") String address,
            @RequestParam(required = false, defaultValue = "") String phone,
            @RequestParam(required = false, defaultValue = "") String email,
            @RequestParam(required = false, defaultValue = "") String role
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
            Page<UserDTO> userDTOPage = userService.filterUsers(username, address, phone, email, role, pageable);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Users retrieved successfully");
            response.put("users", userDTOPage.getContent());
            response.put("currentPage", userDTOPage.getNumber());
            response.put("totalItems", userDTOPage.getTotalElements());
            response.put("totalPages", userDTOPage.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve users: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a user's details by ID")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid or missing Authorization header"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid or expired token"));
            }
            String authEmail = jwtUtil.getEmailFromToken(token);

            Optional<User> userOpt = userService.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with ID: " + id));
            }

            User user = userOpt.get();
            if (!authEmail.equals(user.getEmail()) && !"ADMIN".equals(jwtUtil.getRoleFromToken(token))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Unauthorized to access this user's details"));
            }

            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setAddress(user.getAddress());
            userDTO.setPhone(user.getPhone());
            userDTO.setRole(user.getRole());
            userDTO.setCreatedAt(user.getCreatedAt());

            return ResponseEntity.ok(Map.of("message", "User retrieved successfully", "data", userDTO));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid JWT token: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve user: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT token and user info")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginDTO loginRequest) {
        try {
            if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email cannot be empty"));
            }
            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Password cannot be empty"));
            }

            Optional<User> userOpt = userService.findByEmail(loginRequest.getEmail());
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with email: " + loginRequest.getEmail()));
            }

            User user = userOpt.get();
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid password"));
            }

            String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setAddress(user.getAddress());
            userDTO.setPhone(user.getPhone());
            userDTO.setRole(user.getRole());
            userDTO.setCreatedAt(user.getCreatedAt());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("user", userDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to login: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Update an existing user with optional profile image",
            description = "Update a user with user data (form parameters) and an optional profile image using multipart/form-data.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = UserRequest.class)
                    )
            )
    )
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable String id,
            @ModelAttribute UserRequest request) {
        try {
            // Log received data
            logger.info("Received user data for update: username={}, email={}, password={}, address={}, phone={}, role={}",
                    request.getUsername(), request.getEmail(), request.getPassword(),
                    request.getAddress(), request.getPhone(), request.getRole());
            logger.info("Received profileImage: {}", request.getProfileImage() != null ? request.getProfileImage().getOriginalFilename() : "null");

            // Validate required fields
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Username cannot be empty"));
            }
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email cannot be empty"));
            }

            // Check if user exists
            Optional<User> existingUserOpt = userService.findById(id);
            if (!existingUserOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with ID: " + id));
            }
            User existingUser = existingUserOpt.get();

            // Check for duplicate email (allow same email for this user)
            Optional<User> userWithEmail = userService.findByEmail(request.getEmail());
            if (userWithEmail.isPresent() && !userWithEmail.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Email is already used by another user"));
            }

            // Construct updated User object
            User user = new User();
            user.setId(id);
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(request.getPassword() != null && !request.getPassword().trim().isEmpty()
                    ? passwordEncoder.encode(request.getPassword())
                    : existingUser.getPassword());
            user.setAddress(request.getAddress());
            user.setPhone(request.getPhone());
            user.setRole(request.getRole() != null ? request.getRole() : existingUser.getRole());
            user.setCreatedAt(existingUser.getCreatedAt());

            // Handle image upload
            String profileImageUrl = null;
            MultipartFile profileImage = request.getProfileImage();
            if (profileImage != null && !profileImage.isEmpty()) {
                // Delete old image if it exists
                if (existingUser.getProfileImageUrl() != null) {
                    String oldImagePath = UPLOAD_DIR + existingUser.getProfileImageUrl().replace("/uploads/users/", "");
                    try {
                        Files.deleteIfExists(Paths.get(oldImagePath));
                        logger.info("Deleted old profile image: {}", oldImagePath);
                    } catch (IOException e) {
                        logger.warn("Failed to delete old profile image: {}", oldImagePath, e);
                    }
                }
                // Save new image
                profileImageUrl = saveProfileImage(profileImage);
                logger.info("Profile image saved at: {}", profileImageUrl);
                user.setProfileImageUrl(profileImageUrl);
            } else {
                user.setProfileImageUrl(existingUser.getProfileImageUrl());
            }

            // Save updated user
            User updatedUser = userService.updateUser(id, user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User updated successfully");
            response.put("data", updatedUser);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Error processing file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Error processing file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error in updateUser: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Unexpected error: " + e.getMessage()));
        }
    }


    @PostMapping("/users/change-password")
    @Operation(summary = "Change user password", description = "Change the password without authentication")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordDTO passwordRequest) {
        try {
            String email = passwordRequest.getEmail();

            // Validate newPassword matches confirmNewPassword
            if (!passwordRequest.getNewPassword().equals(passwordRequest.getConfirmNewPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "New password and confirm password must match"));
            }

            boolean success = userService.changePassword(email, passwordRequest.getOldPassword(), passwordRequest.getNewPassword());
            if (!success) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid old password"));
            }

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to change password: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String id) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with ID: " + id));
            }

            // Không xóa file ảnh vì không lưu profileImageUrl trong User
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete user: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/image")
    @Operation(summary = "Get user profile image", description = "Retrieve the profile image for a user")
    public ResponseEntity<?> getProfileImage(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid or missing Authorization header"));
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid or expired token"));
            }
            String authEmail = jwtUtil.getEmailFromToken(token);

            Optional<User> userOpt = userService.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with ID: " + id));
            }

            User user = userOpt.get();
            if (!authEmail.equals(user.getEmail()) && !"ADMIN".equals(jwtUtil.getRoleFromToken(token))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Unauthorized to access this user's image"));
            }

            // Tìm file ảnh dựa trên id (giả sử tên file chứa id)
            String fileNamePattern = id + "_.*\\.(jpg|png|gif)";
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Optional<Path> imagePath = Files.list(uploadPath)
                    .filter(path -> path.getFileName().toString().matches(fileNamePattern))
                    .findFirst();

            if (!imagePath.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No profile image found for user with ID: " + id));
            }

            Path filePath = imagePath.get();
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"))
                    .body(resource);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid JWT token: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve profile image: " + e.getMessage()));
        }
    }
}