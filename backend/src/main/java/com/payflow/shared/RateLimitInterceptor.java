package com.payflow.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService service;
    private final int loginLimit;
    private final int registrationLimit;
    private final int transferLimit;
    private final Duration window;
    private final boolean trustForwardedHeaders;

    public RateLimitInterceptor(RateLimitService service,
                                @Value("${app.rate-limit.login:10}") int loginLimit,
                                @Value("${app.rate-limit.registration:5}") int registrationLimit,
                                @Value("${app.rate-limit.transfer:30}") int transferLimit,
                                @Value("${app.rate-limit.window:1m}") Duration window,
                                @Value("${app.rate-limit.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        this.service = service;
        this.loginLimit = loginLimit;
        this.registrationLimit = registrationLimit;
        this.transferLimit = transferLimit;
        this.window = window;
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        LimitTarget target = target(request);
        if (target == null) {
            return true;
        }
        RateLimitService.Decision decision = service.consume(target.key(), target.limit(), window);
        if (decision.allowed()) {
            return true;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);
        String safeCorrelationId = correlationId == null ? "" : correlationId.toString();
        response.getWriter().write("""
                {"type":"https://payflow.dev/problems/rate-limit-exceeded",\
                "title":"Limite de requisições excedido","status":429,\
                "detail":"Muitas solicitações. Aguarde antes de tentar novamente.",\
                "correlationId":"%s"}
                """.formatted(safeCorrelationId));
        return false;
    }

    private LimitTarget target(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return null;
        }
        return switch (request.getRequestURI()) {
            case "/api/v1/auth/login" -> new LimitTarget("login:" + clientIp(request), loginLimit);
            case "/api/v1/auth/register" -> new LimitTarget("register:" + clientIp(request), registrationLimit);
            case "/api/v1/transfers" -> authenticatedUser(request)
                    .map(subject -> new LimitTarget("transfer:" + subject, transferLimit))
                    .orElse(null);
            default -> null;
        };
    }

    private java.util.Optional<String> authenticatedUser(HttpServletRequest request) {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt && authentication.isAuthenticated()) {
            return java.util.Optional.of(jwt.getToken().getSubject());
        }
        return java.util.Optional.empty();
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private record LimitTarget(String key, int limit) {
    }
}
