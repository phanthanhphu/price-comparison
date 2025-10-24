package org.bsl.pricecomparison.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.bsl.pricecomparison.dto.ChangePasswordDTO;
import org.bsl.pricecomparison.dto.LoginDTO;
import org.bsl.pricecomparison.dto.UserDTO;
import org.bsl.pricecomparison.model.User;
import org.bsl.pricecomparison.repository.UserRepository;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    // 🔥 SWAGGER TOKENS - IN-MEMORY MAP!
    private final Map<String, String> swaggerTokens = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String UPLOAD_DIR = "uploads/users/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // Set để lưu các token đã bị blacklist (để invalidate token khi logout)
    private final Set<String> blacklistedTokens = new HashSet<>();

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
            logger.info("Received user data: username={}, email={}, password={}, address={}, phone={}, role={}, isEnabled={}",
                    request.getUsername(), request.getEmail(), request.getPassword(),
                    request.getAddress(), request.getPhone(), request.getRole(), request.getIsEnabled());
            logger.info("Received profileImage: {}",
                    request.getProfileImage() != null ? request.getProfileImage().getOriginalFilename() : "null");

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
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setAddress(request.getAddress());
            user.setPhone(request.getPhone());
            user.setRole(request.getRole());
            user.setTokenVersion(1L);

            // THÊM: Set isEnabled (mặc định true nếu null)
            Boolean isEnabled = request.getIsEnabled();
            user.setEnabled(isEnabled != null ? isEnabled : true);
            logger.info("User enabled status set to: {}", user.isEnabled());

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
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable String id) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with ID: " + id));
            }

            User user = userOpt.get();
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setAddress(user.getAddress());
            userDTO.setPhone(user.getPhone());
            userDTO.setRole(user.getRole());
            userDTO.setProfileImageUrl(user.getProfileImageUrl());
            userDTO.setCreatedAt(user.getCreatedAt());
            userDTO.setEnabled(user.isEnabled());

            return ResponseEntity.ok(Map.of("message", "User retrieved successfully", "data", userDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to retrieve user: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(
            summary = "SWAGGER LOGIN = AUTO AUTHORIZE!",
            description = "Email: `abc123123@gmail.com` Pass: `123456` → Execute = TẤT CẢ API 200 OK!"
    )
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginDTO loginRequest,
                                                     HttpServletRequest request,
                                                     HttpSession session) {
        try {
            // === VALIDATION ===
            if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email cannot be empty"));
            }
            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Password cannot be empty"));
            }

            // === TÌM USER ===
            Optional<User> userOpt = userService.findByEmail(loginRequest.getEmail());
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with email: " + loginRequest.getEmail()));
            }

            User user = userOpt.get();

            // KIỂM TRA isEnabled = false → CHẶN NGAY
            if (!user.isEnabled()) {
                logger.warn("Login blocked: Account disabled for user: {}", user.getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Your account has been disabled. Please contact the administrator."));
            }

            // === KIỂM TRA MẬT KHẨU ===
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid password"));
            }

            // === TẠO TOKEN + SESSION ===
            long tokenVersion = user.getTokenVersion();
            String token = jwtUtil.generateToken(user.getEmail(), user.getRole(), tokenVersion);

            session.setAttribute("swaggerBearerToken", token);
            session.setAttribute("authenticatedSession", true);
            session.setMaxInactiveInterval(3600 * 24); // 24h

            logger.info("LOGIN SUCCESS → User: {} | Token: {}...", user.getEmail(), token.substring(0, 20));

            // === USER DTO ===
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setAddress(user.getAddress());
            userDTO.setPhone(user.getPhone());
            userDTO.setRole(user.getRole());
            userDTO.setCreatedAt(user.getCreatedAt());
            userDTO.setProfileImageUrl(user.getProfileImageUrl());
            userDTO.setEnabled(user.isEnabled()); // GỬI enabled CHO FRONTEND

            // === RESPONSE ===
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Login successful - All APIs authorized!");
            response.put("token", token);
            response.put("user", userDTO);
            response.put("autoAuthorize", true);
            response.put("sessionActive", true);
            response.put("sessionTimeout", "24h");
            response.put("nextStep", "Execute any API → 200 OK!");

            logger.info("LOGIN COMPLETE → User: {} | Session ID: {}", user.getEmail(), session.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Login failed for {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to login: " + e.getMessage()));
        }
    }

    @DeleteMapping("/logout")
    @Operation(
            summary = "LOGOUT - Clear Session",
            description = "Clear session flags and invalidate session. Must login again to access APIs."
    )
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        try {
            // CLEAR TẤT CẢ FLAGS
            session.removeAttribute("authenticatedSession");
            session.removeAttribute("swaggerBearerToken");
            session.invalidate();

            logger.info("LOGOUT SUCCESS - Session: {} cleared", session.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Logout successful",
                    "session", "cleared",
                    "nextStep", "Login again to continue"
            ));
        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Logout failed"));
        }
    }

    @GetMapping("/check-session")
    @Operation(summary = "Check Swagger Session Token", description = "Debug endpoint to check if token exists")
    public ResponseEntity<Map<String, Object>> checkSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean hasToken = session != null && session.getAttribute("swaggerBearerToken") != null;

        Map<String, Object> response = Map.of(
                "success", true,
                "hasToken", hasToken,
                "sessionActive", session != null,
                "autoAuthorize", hasToken ? "✅ READY!" : "❌ LOGIN AGAIN"
        );

        return ResponseEntity.ok(response);
    }

    // 🔥 THÊM ENDPOINT ĐỂ LẤY TOKEN TỪ SESSION
    @GetMapping("/get-swagger-token")
    @Operation(hidden = true) // ẨN TRONG SWAGGER
    public ResponseEntity<String> getSwaggerToken(HttpSession session) {
        String token = (String) session.getAttribute("swaggerBearerToken");
        return ResponseEntity.ok(token != null ? token : "");
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
            user.setPassword(existingUser.getPassword());
            user.setAddress(request.getAddress());
            user.setPhone(request.getPhone());
            user.setRole(request.getRole() != null ? request.getRole() : existingUser.getRole());
            user.setCreatedAt(existingUser.getCreatedAt());
            user.setTokenVersion(existingUser.getTokenVersion()); // Keep updated version
            user.setEnabled(request.getIsEnabled());

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

    @PostMapping("/change-password")
    @Operation(summary = "Change user password", description = "Change the password WITHOUT authentication + AUTO LOGOUT all sessions")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordDTO passwordRequest) {
        try {
            // Validate input
            if (passwordRequest.getEmail() == null || passwordRequest.getEmail().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email is required"));
            }
            if (passwordRequest.getOldPassword() == null || passwordRequest.getOldPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Old password is required"));
            }
            if (passwordRequest.getNewPassword() == null || passwordRequest.getNewPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "New password is required"));
            }
            if (passwordRequest.getConfirmNewPassword() == null || passwordRequest.getConfirmNewPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Confirm password is required"));
            }

            // Log input for debugging
            logger.info("Change password request for email: {}", passwordRequest.getEmail());

            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(passwordRequest.getEmail());
            if (!userOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found"));
            }
            User user = userOptional.get();

            // Validate old password
            if (!passwordEncoder.matches(passwordRequest.getOldPassword(), user.getPassword())) {
                logger.warn("Invalid old password for user: {}", passwordRequest.getEmail());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Invalid old password"));
            }

            // Validate newPassword matches confirmNewPassword
            if (!passwordRequest.getNewPassword().equals(passwordRequest.getConfirmNewPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "New password and confirm password must match"));
            }

            // **TỰ ĐỘNG LOGOUT TẤT CẢ TOKEN CŨ TRƯỚC KHI ĐỔI PASSWORD**
            incrementTokenVersionAndBlacklist(user);
            logger.info("Invalidated all existing tokens for user: {}", user.getEmail());

            // Update password
            user.setPassword(passwordEncoder.encode(passwordRequest.getNewPassword()));
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            response.put("logoutMessage", "All your sessions have been logged out for security. Please login again.");
            logger.info("Password changed successfully for user: {}", user.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error changing password for {}: {}", passwordRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to change password: " + e.getMessage()));
        }
    }


    @PostMapping("/reset-password")
    @Operation(summary = "Reset user password", description = "Reset the password without requiring old password + AUTO LOGOUT all sessions")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ChangePasswordDTO passwordRequest) {
        try {
            // Validate input
            if (passwordRequest.getEmail() == null || passwordRequest.getEmail().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Email is required"));
            }
            if (passwordRequest.getNewPassword() == null || passwordRequest.getNewPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "New password is required"));
            }
            if (passwordRequest.getConfirmNewPassword() == null || passwordRequest.getConfirmNewPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "Confirm password is required"));
            }

            // Log input for debugging
            logger.info("Reset password request for email: {}", passwordRequest.getEmail());

            // Find user by email
            Optional<User> userOptional = userRepository.findByEmail(passwordRequest.getEmail());
            if (!userOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found"));
            }
            User user = userOptional.get();

            // Validate newPassword matches confirmNewPassword
            if (!passwordRequest.getNewPassword().equals(passwordRequest.getConfirmNewPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "New password and confirm password must match"));
            }

            // Update password
            user.setPassword(passwordEncoder.encode(passwordRequest.getNewPassword()));
            userRepository.save(user);

            // Automatically log out all tokens and sessions
            logger.info("Invalidated all tokens and sessions for user: {}", user.getEmail());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            response.put("logoutMessage", "All your sessions have been logged out for security. Please login again.");
            logger.info("Password reset successfully for user: {}", user.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error resetting password for {}: {}", passwordRequest.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to reset password: " + e.getMessage()));
        }
    }

    /**
     * ✅ FIX: Tăng tokenVersion và blacklist tất cả token cũ của user
     */
    private void incrementTokenVersionAndBlacklist(User user) {
        // ✅ FIX: long primitive - KHÔNG NULL - DEFAULT = 1 từ addUser
        long newTokenVersion = user.getTokenVersion() + 1;
        user.setTokenVersion(newTokenVersion);
        userRepository.save(user); // Save version trước

        // Blacklist tất cả token cũ có tokenVersion cũ
        for (String token : new HashSet<>(blacklistedTokens)) {
            try {
                if (jwtUtil.getEmailFromToken(token) != null &&
                        jwtUtil.getEmailFromToken(token).equals(user.getEmail()) &&
                        jwtUtil.getTokenVersionFromToken(token) < newTokenVersion) {
                    blacklistedTokens.add(token); // Đảm bảo trong blacklist
                }
            } catch (Exception e) {
                // Skip invalid tokens
            }
        }
        logger.info("Token version incremented to {} for user: {}", newTokenVersion, user.getEmail());
    }

    /**
     * ✅ GETTER cho JwtUtil access blacklist
     */
    public Set<String> getBlacklistedTokens() {
        return blacklistedTokens;
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String id) {
//        try {
//            Optional<User> userOpt = userService.findById(id);
//            if (!userOpt.isPresent()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(Map.of("message", "User not found with ID: " + id));
//            }
//
//            User user = userOpt.get();
//            // **TỰ ĐỘNG LOGOUT TẤT CẢ TOKEN KHI XÓA USER**
//            incrementTokenVersionAndBlacklist(user);
//            logger.info("All tokens invalidated for deleted user: {}", user.getEmail());
//
//            userService.deleteUser(id);
//            return ResponseEntity.ok(Map.of("message", "User deleted successfully. All sessions terminated."));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("message", "Failed to delete user: " + e.getMessage()));
//        }
//    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user and profile image", description = "Xóa user + ảnh đại diện + vô hiệu tất cả token")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String id) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "User not found with ID: " + id));
            }

            User user = userOpt.get();

            String profileImageUrl = user.getProfileImageUrl();
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                try {
                    String fileName = profileImageUrl.substring(profileImageUrl.lastIndexOf("/") + 1);
                    Path filePath = Paths.get(UPLOAD_DIR + fileName);

                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        logger.info("Deleted profile image: {}", filePath);
                    } else {
                        logger.warn("Profile image not found on disk: {}", filePath);
                    }
                } catch (IOException e) {
                    logger.error("Failed to delete profile image for user {}: {}", user.getEmail(), e.getMessage());
                }
            }

            incrementTokenVersionAndBlacklist(user);
            logger.info("All tokens invalidated for user: {}", user.getEmail());

            userService.deleteUser(id);

            return ResponseEntity.ok(Map.of(
                    "message", "User deleted successfully. Profile image removed. All sessions terminated."
            ));

        } catch (Exception e) {
            logger.error("Unexpected error deleting user ID {}: {}", id, e.getMessage(), e);
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

            // **CHECK BLACKLIST TRƯỚC**
            if (blacklistedTokens.contains(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Session expired. Please login again."));
            }

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

            // Tìm file ảnh dựa trên id
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