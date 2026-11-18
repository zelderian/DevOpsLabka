package com.example.websecurity26.controller;

import com.example.websecurity26.dto.AuthRequest;
import com.example.websecurity26.dto.AuthResponse;
import com.example.websecurity26.model.AppUser;
import com.example.websecurity26.security.JwtService;
import com.example.websecurity26.service.AppUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AppUserService users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthController(AppUserService users, PasswordEncoder encoder, JwtService jwtService) {
        this.users = users;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/authenticate")
    public AuthResponse authenticate(@RequestBody AuthRequest request) {
        AppUser user = users.findByLogin(request.email())
                .filter(u -> request.password() != null && encoder.matches(request.password(), u.password()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials"));
        return new AuthResponse(jwtService.generateToken(user));
    }
}
