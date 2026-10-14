package com.livecomerce.auth.infrastructure.security;

import com.livecomerce.live.StreamTicketPort;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpHeaders;
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

import java.util.Arrays;
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
    private final StreamTicketPort streamTicketPort;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Comma-separated origins or origin patterns allowed to open the SSE lives feed cross-origin with
     * a stream ticket (e.g. the Vercel production domain and the team-scoped preview pattern).
     * Empty: no cross-origin access to the stream.
     */
    @Value("${app.feed-stream.allowed-origin-patterns:}")
    private String feedStreamAllowedOriginPatterns;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // Async/error re-dispatches of an already-authorized request (e.g. the SSE live
                        // feed): the JWT filter only runs on the initial dispatch, so re-checking would
                        // reject a stream that was authorized when it started.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/stores", "/api/stores/plans", "/api/stores/address-types",
                                "/api/stores/{slug}", "/api/stores/ranking", "/api/stores/{storeId}/reviews",
                                "/api/stores/{storeId}/followers/count", "/api/stores/{storeId}/following").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/browse", "/api/products/{id}", "/api/products/{id}/reviews").permitAll()
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
                // Ticket filter first: JwtAuthFilter only authenticates an empty security context.
                .addFilterBefore(new StreamTicketAuthFilter(streamTicketPort, userDetailsAdapter,
                                restAuthenticationEntryPoint),
                        UsernamePasswordAuthenticationFilter.class)
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
        // The SSE stream (/api/lives/feed/stream) has its own configuration: see feedStreamCorsConfiguration.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        // X-Feed-Version: live-feed snapshot version on GET /api/lives/active, read by the frontend.
        config.setExposedHeaders(List.of("Authorization", "X-Feed-Version"));

        var source = new UrlBasedCorsConfigurationSource();
        // First match wins: the stream path must be registered before the global pattern.
        source.registerCorsConfiguration(StreamTicketAuthFilter.STREAM_PATH, feedStreamCorsConfiguration());
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * The browser opens the SSE stream directly on this backend with a single-use ticket (no cookie,
     * no Bearer), so no credentials are needed. Max-Age caches the preflight across reconnects.
     */
    private CorsConfiguration feedStreamCorsConfiguration() {
        var config = new CorsConfiguration();
        config.setAllowedOriginPatterns(parseOriginPatterns(feedStreamAllowedOriginPatterns));
        config.setAllowCredentials(false);
        config.setAllowedMethods(List.of("GET"));
        config.setAllowedHeaders(List.of(StreamTicketAuthFilter.TICKET_HEADER, "Last-Event-ID"));
        // Not CORS-safelisted: without this the browser can't read the back-off of a 503.
        config.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        config.setMaxAge(600L);
        return config;
    }

    private static List<String> parseOriginPatterns(String value) {
        if (value == null) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
