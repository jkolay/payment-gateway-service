package com.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Dev-mode tokens mapped to client IDs for multi-tenant testing.
     */
    private static final Map<String, String> DEV_TOKENS = Map.of(
            "client-a-token", "client-a",
            "client-b-token", "client-b"
    );

    private static void applyCommon(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/error",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2-console/**",
                                "/actuator/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));
    }

    /**
     * Default security: OAuth2 Resource Server (JWT).
     * Extracts client_id from JWT claims for multi-tenant isolation.
     */
    @Bean
    @Profile("!dev")
    SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        applyCommon(http);
        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setPrincipalClaimName("client_id");
            jwt.jwtAuthenticationConverter(converter);
        }));
        return http.build();
    }

    /**
     * Dev-only security: accept fixed bearer tokens mapped to client IDs.
     * Supports multi-tenant testing without a real IdP.
     */
    @Bean
    @Profile("dev")
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        applyCommon(http);
        http.addFilterBefore(
                new DevBearerTokenFilter(),
                org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class
        );
        return http.build();
    }

    private static class DevBearerTokenFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) {
                String token = auth.substring("Bearer ".length()).trim();
                String clientId = DEV_TOKENS.get(token);
                if (clientId != null) {
                    AbstractAuthenticationToken authentication = new PreAuthenticatedAuthenticationToken(
                            clientId,
                            token,
                            List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))
                    );
                    authentication.setAuthenticated(true);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }

            filterChain.doFilter(request, response);
        }
    }
}
