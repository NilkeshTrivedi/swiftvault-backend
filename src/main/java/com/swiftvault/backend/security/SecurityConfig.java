package com.swiftvault.backend.config;

import com.swiftvault.backend.security.JwtAuthenticationFilter;
import com.swiftvault.backend.service.impl.UserDetailsServiceImpl;
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

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl  userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter   = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password Encoder
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authentication Provider
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Authentication Manager
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORS
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Security Filter Chain
    // ─────────────────────────────────────────────────────────────────────────

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

                        // ── Public endpoints ────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh-token").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ── Auth (authenticated user) ───────────────────────────────
                        .requestMatchers(HttpMethod.GET,  "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/set-transaction-pin").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-transaction-pin").authenticated()

                        // ── Users ───────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,  "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.PUT,  "/api/users/profile").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/users/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/users/{userId}").hasRole("ADMIN")

                        // ── Accounts ────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/accounts").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/accounts").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/accounts/{accountNumber}").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/accounts/{accountNumber}/balance").authenticated()

                        // Phase 3B — Self-Freeze
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/self-freeze").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/self-unfreeze").authenticated()

                        // Admin — Account management
                        .requestMatchers(HttpMethod.PUT,  "/api/accounts/{accountNumber}/freeze").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/accounts/{accountNumber}/unfreeze").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/accounts/admin/all").hasRole("ADMIN")

                        // ── Transactions ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/deposit").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/withdraw").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/accounts/*/transfer").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/accounts/*/transactions").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/transactions/{transactionId}").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/transactions/all").hasRole("ADMIN")

                        // ── Fixed Deposits ──────────────────────────────────────────
                        .requestMatchers("/api/fixed-deposits/**").authenticated()

                        // ── Recurring Deposits ──────────────────────────────────────
                        .requestMatchers("/api/recurring-deposits/**").authenticated()

                        // ── Loans ───────────────────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/loans/apply").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/loans").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/loans/{loanId}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/loans/{loanId}/repay").authenticated()
                        .requestMatchers(HttpMethod.PUT,  "/api/loans/{loanId}/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,  "/api/loans/{loanId}/reject").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,  "/api/loans/admin/all").hasRole("ADMIN")

                        // ── Virtual Cards ───────────────────────────────────────────
                        .requestMatchers("/api/cards/**").authenticated()

                        // ── Phase 3A: Spending Analytics ────────────────────────────
                        .requestMatchers("/api/analytics/**").authenticated()

                        // ── Phase 3A: Statement PDF Export ──────────────────────────
                        .requestMatchers("/api/statement/**").authenticated()

                        // ── Phase 3A: Savings Goals ─────────────────────────────────
                        .requestMatchers("/api/goals/**").authenticated()

                        // ── Phase 3A: Login Device Tracking ─────────────────────────
                        .requestMatchers("/api/devices/**").authenticated()

                        // ── Phase 3A: Admin Audit Log ────────────────────────────────
                        .requestMatchers("/api/admin/audit/**").hasRole("ADMIN")

                        // ── Phase 3B: Suspicious Activity Alerts ────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/fraud/my-alerts").authenticated()
                        .requestMatchers("/api/fraud/admin/**").hasRole("ADMIN")

                        // ── Phase 3B: Transaction Limit Controls ─────────────────────
                        .requestMatchers("/api/limits/**").authenticated()

                        // ── Phase 3B: Auto Savings Rules ─────────────────────────────
                        .requestMatchers("/api/auto-savings/**").authenticated()

                        // ── Phase 3B: Low Balance Alerts ─────────────────────────────
                        .requestMatchers("/api/alerts/**").authenticated()

                        // ── Phase 3B: Referral System ────────────────────────────────
                        .requestMatchers("/api/referral/**").authenticated()

                        // ── Admin: General ───────────────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ── Catch-all ────────────────────────────────────────────────
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}