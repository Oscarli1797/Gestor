package com.GestorProyectos.security;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.GestorProyectos.dto.ApiResponse;
import com.GestorProyectos.repository.UserRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    @Autowired
    private UserRepositoryAuthenticationProvider authenticationProvider;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepository;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;

    @Autowired
    private OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Value("${app.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/auth/register", "/api/auth/verify", "/api/auth/login",
                "/api/auth/forgot-password", "/api/auth/reset-password",
                "/api/stripe/webhook",
                "/oauth2/**", "/login/oauth2/**"
            ).permitAll()
            .anyRequest().authenticated()
        );

        http.formLogin(form -> form
            .loginProcessingUrl("/api/auth/login")
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler((req, res, authentication) -> {
                String token = jwtTokenProvider.generateToken(authentication.getName());
                res.setStatus(HttpServletResponse.SC_OK);
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(res.getWriter(),
                    ApiResponse.ok("Login successful", token));
            })
            .failureHandler((req, res, ex) -> {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(res.getWriter(),
                    ApiResponse.error("Invalid username or password"));
            })
        );

        http.logout(logout -> logout
            .logoutUrl("/api/auth/logout")
            .logoutSuccessHandler((req, res, authentication) -> {
                res.setStatus(HttpServletResponse.SC_OK);
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(res.getWriter(),
                    ApiResponse.ok("Logged out", null));
            })
        );

        http.exceptionHandling(ex -> ex
            .authenticationEntryPoint((req, res, authEx) -> {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(res.getWriter(),
                    ApiResponse.error("Authentication required"));
            })
            .accessDeniedHandler((req, res, accessEx) -> {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(res.getWriter(),
                    ApiResponse.error("Access denied"));
            })
        );

        http.oauth2Login(oauth2 -> oauth2
            .authorizationEndpoint(ae -> ae
                .baseUri("/oauth2/authorization")
                .authorizationRequestRepository(cookieAuthRequestRepository))
            .redirectionEndpoint(re -> re
                .baseUri("/login/oauth2/code/*"))
            .successHandler(oAuth2SuccessHandler)
            .failureHandler(oAuth2FailureHandler)
        );

        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.authenticationProvider(authenticationProvider);
        return builder.build();
    }
}
