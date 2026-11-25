package com.atomicpay.backend.security;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class AuthTokenService {

    private final ConcurrentHashMap<String, String> tokens =
            new ConcurrentHashMap<>();

    public String generate(String username) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, username);
        return token;
    }

    public String getUsername(String token) {
        return tokens.get(token);
    }
}
