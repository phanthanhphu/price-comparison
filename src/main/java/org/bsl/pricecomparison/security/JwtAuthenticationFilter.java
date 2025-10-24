package org.bsl.pricecomparison.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String SESSION_AUTH_FLAG = "authenticatedSession";
    private static final String SWAGGER_TOKEN = "swaggerBearerToken";

    // Danh sách các endpoint công khai (phải khớp với SecurityConfig)
    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/swagger-ui/",
            "/v3/api-docs",
            "/v3/api-docs/",
            "/swagger-resources/",
            "/webjars/",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/health",
            "/info",
            "/actuator/",
            "/api/product-type-1/search",
            "/api/product-type-2/search",
            "/api/group-summary-requisitions/",
            "/api/departments/filter",
            "/api/supplier-products/filter",
            "/search/comparison-monthly"
    );

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("PROCESSING → {} {}", method, path);

        // BƯỚC 0: STATIC RESOURCES & UPLOADS - BYPASS TRƯỚC NHẤT
        if (isStaticResource(path)) {
            logger.debug("STATIC BYPASS → {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // BƯỚC 1: PUBLIC ENDPOINTS - BYPASS
        if (isPublicEndpoint(path, method)) {
            logger.debug("PUBLIC BYPASS → {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // BƯỚC 2: LOGIN ENDPOINTS - BYPASS
        if (isLoginEndpoint(path, method)) {
            logger.info("LOGIN BYPASS → {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // BƯỚC 3: GLOBAL SESSION AUTH FLAG - CHỈ API CALLS
        HttpSession session = request.getSession(false);
        Boolean authFlag = (Boolean) (session != null ? session.getAttribute(SESSION_AUTH_FLAG) : null);

        if (authFlag != null && authFlag && isApiEndpoint(path)) {
            logger.info("GLOBAL BYPASS → Session: {} | API: {}", session != null ? session.getId() : "null", path);
            String token = getTokenFromSession(request, SWAGGER_TOKEN);
            if (token != null && !token.trim().isEmpty()) {
                try {
                    if (jwtUtil.validateToken(token)) {
                        String userEmail = jwtUtil.getEmailFromToken(token);
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userEmail, null, new ArrayList<>());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.info("SET AUTHENTICATION → User: {} | Session: {}", userEmail, session.getId());
                    }
                } catch (Exception e) {
                    logger.error("TOKEN VALIDATION ERROR → Path: {} | Error: {}", path, e.getMessage());
                }
            }
            filterChain.doFilter(request, response);
            return;
        }

        // BƯỚC 4: BEARER TOKEN VALIDATION - SET GLOBAL FLAG
        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            logger.debug("BEARER FOUND → Path: {} | Token Length: {}", path, token.length());
        }

        if (token != null && !token.trim().isEmpty()) {
            try {
                if (jwtUtil.validateToken(token)) {
                    String userEmail = jwtUtil.getEmailFromToken(token);
                    logger.info("BEARER VALID → User: {} | Path: {}", userEmail, path);

                    if (session == null) {
                        session = request.getSession(true);
                    }
                    session.setAttribute(SESSION_AUTH_FLAG, true);
                    session.setMaxInactiveInterval(3600 * 24);
                    logger.info("BEARER → GLOBAL FLAG SET → Session: {} | User: {}", session.getId(), userEmail);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userEmail, null, new ArrayList<>());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("SET AUTHENTICATION → User: {} | Session: {}", userEmail, session.getId());

                    filterChain.doFilter(request, response);
                    return;
                } else {
                    logger.warn("BEARER INVALID → Path: {} | Token: {}", path, token.substring(0, Math.min(20, token.length())) + "...");
                }
            } catch (Exception e) {
                logger.error("BEARER ERROR → Path: {} | Error: {}", path, e.getMessage());
            }
        }

        // BƯỚC 5: SWAGGER API CALL - AUTO INJECT + SET GLOBAL FLAG
        if (isSwaggerApiCall(request)) {
            logger.info("SWAGGER DETECTED → {}", path);
            String swaggerToken = getTokenFromSession(request, SWAGGER_TOKEN);

            if (swaggerToken != null && !swaggerToken.trim().isEmpty()) {
                try {
                    if (jwtUtil.validateToken(swaggerToken)) {
                        if (session == null) {
                            session = request.getSession(true);
                        }
                        session.setAttribute(SESSION_AUTH_FLAG, true);
                        session.setMaxInactiveInterval(3600 * 24);

                        logger.info("SWAGGER → GLOBAL FLAG SET → Session: {} | Token OK", session.getId());

                        String userEmail = jwtUtil.getEmailFromToken(swaggerToken);
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userEmail, null, new ArrayList<>());
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.info("SWAGGER AUTH SET → User: {} | Session: {}", userEmail, session.getId());

                        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                            @Override
                            public String getHeader(String name) {
                                if ("Authorization".equalsIgnoreCase(name)) {
                                    logger.info("SWAGGER TOKEN INJECTED → {}", path);
                                    return "Bearer " + swaggerToken;
                                }
                                return super.getHeader(name);
                            }
                        };

                        filterChain.doFilter(wrappedRequest, response);
                        return;
                    } else {
                        logger.warn("SWAGGER UNAUTH → Invalid token: {}", path);
                        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Swagger: Invalid token");
                        return;
                    }
                } catch (Exception e) {
                    logger.error("SWAGGER TOKEN ERROR → Path: {} | Error: {}", path, e.getMessage());
                    sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Swagger: Token validation failed");
                    return;
                }
            }

            logger.warn("SWAGGER UNAUTH → Missing token: {}", path);
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Swagger: Login first at /swagger-login");
            return;
        }

        // KHÔNG AUTH → 401
        logger.warn("FULLY UNAUTHORIZED → Path: {} | No valid auth method", path);
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Please login first");
    }

    private boolean isStaticResource(String path) {
        if (path.startsWith("/uploads/")) {
            logger.trace("UPLOADS BYPASS → {}", path);
            return true;
        }

        if (path.matches("/(assets|static|public|css|js|images)/.*")) {
            logger.trace("ASSETS BYPASS → {}", path);
            return true;
        }

        if (path.matches(".*\\.(css|js|png|jpg|jpeg|gif|webp|ico|svg|woff2?|ttf|eot|pdf|zip|gz|json|xml|txt|html)$")) {
            logger.trace("FILE BYPASS → {}", path);
            return true;
        }

        return "/favicon.ico".equals(path) ||
                "/".equals(path) ||
                "/index.html".equals(path);
    }

    private boolean isApiEndpoint(String path) {
        return path.startsWith("/api/") ||
                path.startsWith("/users/") && !path.equals("/users/login") ||
                path.startsWith("/products/") ||
                path.startsWith("/departments/") ||
                path.startsWith("/supplier-products/") ||
                path.startsWith("/categories/") ||
                path.startsWith("/orders/") ||
                path.startsWith("/requisition-monthly/") ||
                path.startsWith("/summary-requisitions/") ||
                path.startsWith("/requisitions/") ||
                path.startsWith("/inventory/") ||
                path.startsWith("/warehouse/");
    }

    private boolean isLoginEndpoint(String path, String method) {
        return switch (path) {
            case "/login", "/users/login", "/api/auth/login", "/swagger-login" -> method.equals("POST");
            default -> false;
        };
    }

    private boolean isPublicEndpoint(String path, String method) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(endpoint -> path.startsWith(endpoint)) ||
                path.matches("/actuator/.*");
    }

    private boolean isSwaggerApiCall(HttpServletRequest request) {
        String path = request.getRequestURI();
        String referer = request.getHeader("Referer");
        String userAgent = request.getHeader("User-Agent");
        String swaggerToken = getTokenFromSession(request, SWAGGER_TOKEN);

        String refererDisplay = referer != null ? (referer.length() > 30 ? referer.substring(0, 30) + "..." : referer) : "null";
        String tokenDisplay = swaggerToken != null ? (swaggerToken.length() > 20 ? swaggerToken.substring(0, 20) + "..." : swaggerToken) : "null";

        logger.debug("SWAGGER CHECK → Path: {} | Referer: {} | UserAgent: {} | SwaggerToken: {} | HasToken: {}",
                path, refererDisplay, userAgent != null ? userAgent : "null", tokenDisplay, swaggerToken != null);

        if (isLoginEndpoint(path, request.getMethod())) return false;

        boolean isApiCall = path.startsWith("/api/") ||
                path.startsWith("/users/") && !path.equals("/users/login") ||
                path.startsWith("/products/") ||
                path.startsWith("/departments/") ||
                path.startsWith("/categories/") ||
                path.startsWith("/orders/") ||
                path.startsWith("/requisition-monthly/") ||
                path.startsWith("/summary-requisitions/") ||
                path.startsWith("/supplier-products/") ||
                path.startsWith("/requisitions/") ||
                path.startsWith("/inventory/") ||
                path.startsWith("/warehouse/") ||
                "/users".equals(path) ||
                (path.startsWith("/users/") && path.contains("?"));

        boolean fromSwaggerUI = referer != null && (
                referer.contains("swagger-ui") ||
                        referer.contains("swagger-ui.html") ||
                        referer.contains("/swagger-ui/") ||
                        referer.contains("/v3/api-docs")
        );

        boolean swaggerUA = userAgent != null && (
                userAgent.contains("Swagger") ||
                        (userAgent.contains("Chrome/") && fromSwaggerUI)
        );

        boolean hasSwaggerToken = swaggerToken != null;

        boolean isSwagger = isApiCall && (fromSwaggerUI || swaggerUA || hasSwaggerToken);

        logger.debug("SWAGGER RESULT → IsApiCall: {} | FromSwaggerUI: {} | SwaggerUA: {} | HasSwaggerToken: {} | IsSwagger: {}",
                isApiCall, fromSwaggerUI, swaggerUA, hasSwaggerToken, isSwagger);

        return isSwagger;
    }

    private String getTokenFromSession(HttpServletRequest request, String tokenAttribute) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String token = (String) session.getAttribute(tokenAttribute);
                return token != null && !token.trim().isEmpty() ? token : null;
            }
        } catch (Exception e) {
            logger.error("Session token error [{}]: {}", tokenAttribute, e.getMessage());
        }
        return null;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("status", status.value());
        error.put("timestamp", System.currentTimeMillis());

        ObjectMapper mapper = new ObjectMapper();
        String jsonResponse = mapper.writeValueAsString(error);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.matches("^/error.*|/actuator.*|/health$");
    }
}