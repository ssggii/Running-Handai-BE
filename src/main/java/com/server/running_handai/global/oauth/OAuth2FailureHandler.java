package com.server.running_handai.global.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest,
                                        HttpServletResponse httpServletResponse,
                                        AuthenticationException authenticationException)
            throws IOException {
        log.warn("[OAuth2 인증] 실패 - 오류: {}", authenticationException.getMessage());

        String clientRedirectUrl =
                redirectUrl
                        + "?result=false"
                        + "&exception="
                        + URLEncoder.encode(authenticationException.getMessage(), StandardCharsets.UTF_8);

        httpServletResponse.sendRedirect(clientRedirectUrl);
    }
}