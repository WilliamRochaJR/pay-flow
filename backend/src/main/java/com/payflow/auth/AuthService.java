package com.payflow.auth;

import com.payflow.accounts.Account;
import com.payflow.accounts.AccountRepository;
import com.payflow.shared.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AccountRepository accountRepository;
    private final String dummyPasswordHash;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, TokenService tokenService,
                       AccountRepository accountRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.accountRepository = accountRepository;
        this.dummyPasswordHash = passwordEncoder.encode("password-used-only-for-timing-protection");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmail(email)) {
            throw emailAlreadyRegistered();
        }
        User user = User.register(request.name(), email, passwordEncoder.encode(request.password()));
        try {
            User savedUser = repository.saveAndFlush(user);
            accountRepository.saveAll(List.of(
                    Account.demo(savedUser.getId(), savedUser.getName() + " • Principal", new BigDecimal("2500.00")),
                    Account.demo(savedUser.getId(), savedUser.getName() + " • Reserva", new BigDecimal("1000.00"))
            ));
            return UserResponse.from(savedUser);
        } catch (DataIntegrityViolationException exception) {
            throw emailAlreadyRegistered();
        }
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = repository.findByEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return tokenService.issue(user);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(String subject) {
        try {
            return repository.findById(java.util.UUID.fromString(subject))
                    .map(UserResponse::from)
                    .orElseThrow(AuthService::invalidCredentials);
        } catch (IllegalArgumentException exception) {
            throw invalidCredentials();
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static BusinessException invalidCredentials() {
        return new BusinessException("invalid-credentials", "E-mail ou senha inválidos.");
    }

    private static BusinessException emailAlreadyRegistered() {
        return new BusinessException("email-already-registered", "Este e-mail já está cadastrado.");
    }
}
