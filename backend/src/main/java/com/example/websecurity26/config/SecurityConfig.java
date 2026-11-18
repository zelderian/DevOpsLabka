package com.example.websecurity26.config;

import com.example.websecurity26.security.JwtAuthenticationFilter;
import com.example.websecurity26.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http, JwtService jwtService,
                              @Value("${app.csrf.enabled:true}") boolean csrfEnabled) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/error", "/health", "/version").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/authenticate").permitAll()
                .requestMatchers("/api/v1/annotated/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/items/**").hasAnyRole("ADMIN", "MANAGER", "USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/items/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/items/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/items/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService), BasicAuthenticationFilter.class);

        if (csrfEnabled) {
            http.csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                    .ignoringRequestMatchers(req -> "/api/v1/auth/authenticate".equals(req.getRequestURI())
                            || JwtAuthenticationFilter.hasBearer(req)))
                .addFilterAfter(new CsrfHeaderFilter(), CsrfFilter.class);
        } else {
            http.csrf(csrf -> csrf.disable());
        }
        return http.build();
    }
}
