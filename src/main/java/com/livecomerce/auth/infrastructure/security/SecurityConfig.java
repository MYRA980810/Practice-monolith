package com.livecomerce.auth.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
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
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtService jwtService;
    private final UserDetailsAdapter userDetailsAdapter;
    private final GoogleOAuth2SuccessHandler googleOAuth2SuccessHandler;
    private final CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stores", "/api/stores/plans", "/api/stores/address-types", "/api/stores/{slug}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories").permitAll()
                        .requestMatchers("/api/billing/webhook/**").permitAll()
                        .requestMatchers("/api/payment/webhook").permitAll()
                        .requestMatchers("/api/webhooks/agora/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/lives/*/heartbeat").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(ep -> ep
                                .baseUri("/api/auth/oauth2/authorization")
                                .authorizationRequestRepository(authorizationRequestRepository))
                        .redirectionEndpoint(ep -> ep
                                .baseUri("/api/auth/oauth2/callback/*"))
                        .successHandler(googleOAuth2SuccessHandler)
                        .failureHandler((req, res, ex) -> {
                            log.warn("OAuth2 login failed", ex);
                            res.sendRedirect(frontendUrl + "/auth/login?error=oauth");
                        })
                )
                .addFilterBefore(new JwtAuthFilter(jwtService, userDetailsAdapter),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setExposedHeaders(List.of("Authorization"));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
