package com.payflow.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String allowedOrigin;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebConfig(@Value("${app.cors.allowed-origin:http://localhost:5173}") String allowedOrigin,
                     RateLimitInterceptor rateLimitInterceptor) {
        this.allowedOrigin = allowedOrigin;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Authorization", "Content-Type", "Idempotency-Key", CorrelationIdFilter.HEADER_NAME)
                .exposedHeaders(CorrelationIdFilter.HEADER_NAME);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/v1/**");
    }
}
