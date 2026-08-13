package com.payflow.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String ATTRIBUTE_NAME = CorrelationIdFilter.class.getName() + ".correlationId";
    private static final String MDC_KEY = "correlationId";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request.getHeader(HEADER_NAME));
        request.setAttribute(ATTRIBUTE_NAME, correlationId);
        response.setHeader(HEADER_NAME, correlationId);
        long startedAt = System.nanoTime();

        try (MDC.MDCCloseable ignored = MDC.putCloseable(MDC_KEY, correlationId)) {
            try {
                filterChain.doFilter(request, response);
            } finally {
                long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                log.atInfo()
                        .addKeyValue("method", request.getMethod())
                        .addKeyValue("path", request.getRequestURI())
                        .addKeyValue("status", response.getStatus())
                        .addKeyValue("durationMs", durationMs)
                        .log("http_request");
            }
        }
    }

    private String resolveCorrelationId(String candidate) {
        if (candidate != null) {
            try {
                return UUID.fromString(candidate).toString();
            } catch (IllegalArgumentException ignored) {
                // An invalid external value is replaced to prevent untrusted content from reaching logs.
            }
        }
        return UUID.randomUUID().toString();
    }
}
