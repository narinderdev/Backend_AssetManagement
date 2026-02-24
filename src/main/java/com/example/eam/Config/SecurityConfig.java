package com.example.eam.Config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.eam.Auth.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Comma-separated list of allowed origins. Defaults cover local dev and the
     * current public IP/ports for HTTP-only deployment.
     */
    @Value("${app.cors.allowed-origins:http://localhost:4200,http://localhost:4300,http://54.225.63.207:4300,http://54.225.63.207,http://3.84.76.210:4300,http://3.84.76.210}")
    private String allowedOrigins;

    /**
     * Comma-separated list of allowed origin patterns (supports wildcards) for dynamic hosts like ngrok.
     */
    @Value("${app.cors.allowed-origin-patterns:https://*.ngrok-free.app,http://*.ngrok-free.app}")
    private String allowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/", "/health").permitAll()
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/auth",
                        "/auth/",
                        "/auth/login/mfa",
                        "/auth/mfa/email/send",
                        "/auth/mfa/email/verify"
                ).permitAll()
                .requestMatchers("/auth/signup/**").permitAll()
                .requestMatchers(
                        "/users/accept",
                        "/users/set-password"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/users", "/users/").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/change-password", "/users/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/users/invite").hasRole("Admin")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(resolveAllowedOrigins());
        config.setAllowedOriginPatterns(resolveAllowedOriginPatterns());
        config.setAllowCredentials(true);

        config.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        config.setAllowedHeaders(List.of("*"));  // Allow all headers

        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition"
        ));

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> resolveAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> resolveAllowedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
