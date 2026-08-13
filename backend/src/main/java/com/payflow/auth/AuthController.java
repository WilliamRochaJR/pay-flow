package com.payflow.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Autenticação", description = "Cadastro, login e usuário autenticado")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/auth/register")
    @Operation(summary = "Cadastrar usuário")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = service.register(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Autenticar e emitir access token")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.findById(jwt.getSubject());
    }
}
