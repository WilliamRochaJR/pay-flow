package com.payflow.shared;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

    @Test
    void returnsProblemDetailsAndRetryAfterWhenLoginLimitIsExceeded() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(
                new RateLimitService(), 1, 1, 1, Duration.ofMinutes(1), false
        );
        MockHttpServletRequest first = loginRequest("198.51.100.10");
        assertThat(interceptor.preHandle(first, new MockHttpServletResponse(), new Object())).isTrue();

        MockHttpServletRequest repeated = loginRequest("198.51.100.10");
        repeated.setAttribute(CorrelationIdFilter.ATTRIBUTE_NAME, "6b0d1a32-1083-4f56-b779-dc01d8eb0a28");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(repeated, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getContentAsString()).contains(
                "rate-limit-exceeded", "6b0d1a32-1083-4f56-b779-dc01d8eb0a28"
        );
    }

    @Test
    void ignoresForwardedIpWhenProxyTrustIsDisabled() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(
                new RateLimitService(), 1, 1, 1, Duration.ofMinutes(1), false
        );
        MockHttpServletRequest first = loginRequest("203.0.113.5");
        first.addHeader("X-Forwarded-For", "198.51.100.1");
        MockHttpServletRequest repeated = loginRequest("203.0.113.5");
        repeated.addHeader("X-Forwarded-For", "198.51.100.2");

        assertThat(interceptor.preHandle(first, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(interceptor.preHandle(repeated, new MockHttpServletResponse(), new Object())).isFalse();
    }

    @Test
    void appliesTheAuthenticatedTransferLimitToReversals() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(
                new RateLimitService(), 1, 1, 1, Duration.ofMinutes(1), false
        );
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("f8b5364a-164f-4bb6-8056-c896d13eb5d4")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt,
                java.util.Collections.emptyList(),
                jwt.getSubject()
        ));

        try {
            MockHttpServletRequest transfer = new MockHttpServletRequest("POST", "/api/v1/transfers");
            MockHttpServletRequest reversal = new MockHttpServletRequest(
                    "POST",
                    "/api/v1/transfers/0f9709f2-787f-441f-9a55-13c854778766/reversals"
            );

            assertThat(interceptor.preHandle(transfer, new MockHttpServletResponse(), new Object())).isTrue();
            assertThat(interceptor.preHandle(reversal, new MockHttpServletResponse(), new Object())).isFalse();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private MockHttpServletRequest loginRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
