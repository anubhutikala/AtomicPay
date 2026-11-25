package com.atomicpay.backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atomicpay.backend.model.Account;
import com.atomicpay.backend.model.AppUser;
import com.atomicpay.backend.repository.AccountRepository;
import com.atomicpay.backend.repository.AppUserRepository;
import com.atomicpay.backend.security.AuthTokenService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AppUserRepository users;
    private final AccountRepository accounts;
    private final PasswordEncoder encoder;
    private final AuthTokenService tokens;

    public AuthController(
            AppUserRepository users,
            AccountRepository accounts,
            PasswordEncoder encoder,
            AuthTokenService tokens) {
        this.users = users;
        this.accounts = accounts;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {

        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");

        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || password == null || password.length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid registration details."));
        }

        if (users.existsByUsername(email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email already registered."));
        }

        String userId = nextId(users.findTopByOrderByUserIdDesc()
                .map(AppUser::getUserId).orElse("U000"), "U");

        String accountId = nextId(accounts.findTopByOrderByAccountIdDesc()
                .map(Account::getAccountId).orElse("A000"), "A");

        AppUser user = new AppUser();
        user.setUserId(userId);
        user.setUsername(email);
        user.setPassword(encoder.encode(password));
        user.setRole("USER");

        users.save(user);

        Account account = new Account();
        account.setAccountId(accountId);
        account.setUserId(userId);
        account.setName(name);
        account.setBalance(new java.math.BigDecimal("10000.00"));

        accounts.save(account);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        return authenticate(body, false);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(
            @RequestBody Map<String, String> body) {
        return authenticate(body, true);
    }

    private ResponseEntity<?> authenticate(
            Map<String, String> body, boolean adminOnly) {

        String username = body.get("email");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Username and password required."));
        }

        AppUser user = users.findByUsername(username).orElse(null);

        if (user == null || !encoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password."));
        }

        if (adminOnly && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Administrator access required."));
        }

        if (!adminOnly && "ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Please use administrator login."));
        }

        Account account = accounts.findByUserId(user.getUserId()).orElse(null);

        String token = tokens.generate(user.getUsername());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole(),
                "userId", user.getUserId(),
                "accountId", account == null ? "" : account.getAccountId(),
                "id", account == null ? "" : account.getAccountId(),
                "name", account == null ? user.getUsername() : account.getName(),
                "email", user.getUsername()
        ));
    }

    private String nextId(String current, String prefix) {
        int number = Integer.parseInt(current.substring(1)) + 1;
        return prefix + String.format("%03d", number);
    }
}