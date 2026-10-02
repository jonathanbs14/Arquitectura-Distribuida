package com.prueba.graftsql.credito.auth.adapter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Perfil local: permite probar los endpoints propios sin registrar proveedores externos. */
    @Bean
    @Profile("!oauth")
    public SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }

    /**
     * Perfil de producción OAuth. Spring Security guarda y verifica el state de OAuth2
     * durante el intercambio del código, por lo que la sesión es necesaria solo para ese flujo.
     */
    @Bean
    @Profile("oauth")
    public SecurityFilterChain oauthFilterChain(HttpSecurity http,
                                                 OAuthLoginSuccessHandler successHandler,
                                                 OAuthLoginFailureHandler failureHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/oauth/**", "/api/auth/**", "/register", "/login", "/mfa/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/oauth/**", "/api/auth/**", "/register", "/login", "/mfa/**", "/oauth2/**", "/login/**",
                                "/actuator/health/**", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                        .failureHandler(failureHandler))
                .build();
    }
}
