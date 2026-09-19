package com.meridiantrust.sentinel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Stateless HTTP Basic security with role-based access control enforced at the
 * API layer. Bootstrap credentials come from configuration/environment
 * (Security NFR — no hardcoded secrets). Fine-grained authorization is applied
 * via {@code @PreAuthorize} on controllers ({@link EnableMethodSecurity}).
 *
 * <p>Roles: ADMIN (config + ingestion), SUPERVISOR (full analyst + PII detail),
 * ANALYST (triage/disposition/cases).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsManager(
            PasswordEncoder encoder,
            @Value("${sentinel.security.users.admin-password}") String adminPassword,
            @Value("${sentinel.security.users.analyst-password}") String analystPassword,
            @Value("${sentinel.security.users.supervisor-password}") String supervisorPassword) {

        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
        UserDetails supervisor = User.withUsername("supervisor")
                .password(encoder.encode(supervisorPassword))
                .roles("SUPERVISOR")
                .build();
        UserDetails analyst = User.withUsername("analyst")
                .password(encoder.encode(analystPassword))
                .roles("ANALYST")
                .build();
        return new InMemoryUserDetailsManager(admin, supervisor, analyst);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/info")
                        .permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> {
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }
}
