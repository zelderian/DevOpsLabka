package com.example.websecurity26.service;

import com.example.websecurity26.model.AppUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AppUserService implements UserDetailsService {
    private final List<AppUser> users;

    public AppUserService(PasswordEncoder encoder) {
        this.users = List.of(
                new AppUser("admin", "admin@mail.com", "Olena Admin", encoder.encode("admin"), "ADMIN"),
                new AppUser("manager", "manager@mail.com", "Petro Manager", encoder.encode("manager"), "MANAGER"),
                new AppUser("user", "user@mail.com", "Taras User", encoder.encode("user"), "USER"));
    }

    public Optional<AppUser> findByLogin(String login) {
        if (login == null) return Optional.empty();
        return users.stream()
                .filter(u -> u.username().equals(login) || u.email().equalsIgnoreCase(login))
                .findFirst();
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        AppUser u = findByLogin(login).orElseThrow(() -> new UsernameNotFoundException(login));
        return User.withUsername(u.username()).password(u.password()).roles(u.role()).build();
    }
}
