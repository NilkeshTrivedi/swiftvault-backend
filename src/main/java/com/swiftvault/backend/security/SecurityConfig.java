package com.swiftvault.backend.security;

import com.swiftvault.backend.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // FIX: Use JwtAuthFilter (the actual class name in this project)
    // FIX: Use UserDetailsService interface — UserServiceImpl satisfies it via UserService.
    //      But UserService interface doesn't extend UserDetailsService.
    //      The User entity implements UserDetails and UserRepository loads it.
    //      Spring Security needs a UserDetailsService bean — we wire it via lambda.
    private final JwtAuthFilter          jwtAuthFilter;
    private final UserDetailsService     userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          UserDetailsService userDetailsService) {
        this.jwtAuthFilter      = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        // FIX: Spring Boot 4 / Spring Security 7 — DaoAuthenticationProvider has no-arg constructor.
        // Must use setters, not constructor args.
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth

                        // ── Public ──────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/fd/rates").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/rd/rates").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/loans/rates").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/loans/calculate-emi").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ── Users ────────────────────────────────────────────────────
                        .requestMatchers("/api/users/me/**").authenticated()
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/users/all").hasRole("ADMIN")

                        // ── Accounts ─────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/accounts").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/accounts").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/accounts/{accountNumber}").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/accounts/{accountNumber}/balance").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/deposit").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/withdraw").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/transfer").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/accounts/*/transactions").authenticated()

                        // Phase 3B — Self-Freeze
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/self-freeze").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/self-unfreeze").authenticated()

                        // Admin — Account management
                        .requestMatchers(HttpMethod.PUT,  "/api/accounts/{accountNumber}/freeze").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/accounts/{accountNumber}/unfreeze").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/accounts/admin/all").hasRole("ADMIN")

                        // ── FD ───────────────────────────────────────────────────────
                        .requestMatchers("/api/fd/**").authenticated()

                        // ── RD ───────────────────────────────────────────────────────
                        .requestMatchers("/api/rd/**").authenticated()

                        // ── Loans ────────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/loans/apply").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/loans").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/loans/{loanId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/loans/pay-emi").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/loans/admin/pending").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/loans/admin/{loanId}/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/loans/admin/{loanId}/reject").hasRole("ADMIN")

                        // ── Cards ────────────────────────────────────────────────────
                        .requestMatchers("/api/cards/**").authenticated()

                        // ── Phase 3A ─────────────────────────────────────────────────
                        .requestMatchers("/api/analytics/**").authenticated()
                        .requestMatchers("/api/statement/**").authenticated()
                        .requestMatchers("/api/goals/**").authenticated()
                        .requestMatchers("/api/devices/**").authenticated()
                        .requestMatchers("/api/admin/audit/**").hasRole("ADMIN")

                        // ── Phase 3B ─────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/fraud/my-alerts").authenticated()
                        .requestMatchers("/api/fraud/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/limits/**").authenticated()
                        .requestMatchers("/api/auto-savings/**").authenticated()
                        .requestMatchers("/api/alerts/**").authenticated()
                        .requestMatchers("/api/referral/**").authenticated()

                        // ── Admin general ─────────────────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ── Catch-all ─────────────────────────────────────────────────
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}