package com.example.websecurity26.controller;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/annotated")
public class AnnotatedController {

    @GetMapping("/hello")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> hello(Authentication auth) {
        return Map.of("message", "Hello, " + auth.getName());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> me(Authentication auth) {
        return Map.of("username", auth.getName(),
                "authorities", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @GetMapping("/user-zone")
    @PreAuthorize("hasRole('USER')")
    public Map<String, String> userZone() {
        return Map.of("zone", "USER only (@PreAuthorize)");
    }

    @GetMapping("/manager-zone")
    @RolesAllowed({"MANAGER", "ADMIN"})
    public Map<String, String> managerZone() {
        return Map.of("zone", "MANAGER or ADMIN (@RolesAllowed)");
    }

    @GetMapping("/admin-zone")
    @Secured("ROLE_ADMIN")
    public Map<String, String> adminZone() {
        return Map.of("zone", "ADMIN only (@Secured)");
    }

    @GetMapping("/profile/{username}")
    @PreAuthorize("#username == authentication.name or hasRole('ADMIN')")
    public Map<String, String> profile(@P("username") @PathVariable String username) {
        return Map.of("profile", username, "rule", "owner or ADMIN (@PreAuthorize + SpEL)");
    }
}
