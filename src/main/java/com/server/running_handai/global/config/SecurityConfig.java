package com.server.running_handai.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.running_handai.global.jwt.JwtAuthenticationFilter;
import com.server.running_handai.global.oauth.OAuth2FailureHandler;
import com.server.running_handai.global.oauth.OAuth2SuccessHandler;
import com.server.running_handai.global.oauth.OAuth2UserService;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import com.server.running_handai.global.oauth.CustomAuthorizationRequestResolver;

import java.io.IOException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Bean
    public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        return new CustomAuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver) throws Exception {
        httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 ->
                        oauth2
                                .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                                .successHandler(oAuth2SuccessHandler)
                                .failureHandler(oAuth2FailureHandler)
                                .authorizationEndpoint(endpoint ->
                                        endpoint.authorizationRequestResolver(customAuthorizationRequestResolver)
                                )
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            returnErrorResponse(response, ResponseCode.UNAUTHORIZED_ACCESS);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            returnErrorResponse(response, ResponseCode.ACCESS_DENIED);
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").permitAll()
                        .requestMatchers(
                                "/health",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/v3/api-docs/**",
                                "/api/members/oauth/token"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    private void returnErrorResponse(HttpServletResponse response, ResponseCode code) throws IOException {
        response.setStatus(code.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        CommonResponse<Object> errorResponse = CommonResponse.error(code);

        ObjectMapper objectMapper = new ObjectMapper();
        String responseBody = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(responseBody);
    }
}