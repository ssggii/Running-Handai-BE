package com.server.running_handai.global.oauth;

import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

@Slf4j
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, String baseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, baseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest httpServletRequest) {
        OAuth2AuthorizationRequest oAuth2AuthorizationRequest = defaultResolver.resolve(httpServletRequest);
        return customizeAuthorizationRequest(httpServletRequest, oAuth2AuthorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest httpServletRequest, String clientRegistrationId) {
        OAuth2AuthorizationRequest oAuth2AuthorizationRequest = defaultResolver.resolve(httpServletRequest, clientRegistrationId);
        return customizeAuthorizationRequest(httpServletRequest, oAuth2AuthorizationRequest);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(HttpServletRequest httpServletRequest, OAuth2AuthorizationRequest oAuth2AuthorizationRequest) {
        if (oAuth2AuthorizationRequest == null) {
            return null;
        }

        String state = httpServletRequest.getParameter("state");
        if (!StringUtils.hasText(state)) {
            log.warn("[OAuth2 인증] 로그인 시 state가 비어있습니다.");
            throw new BusinessException(ResponseCode.BAD_REQUEST_STATE_PARAMETER);
        }

        return OAuth2AuthorizationRequest.from(oAuth2AuthorizationRequest)
                .state(state) // 전달된 state 값을 저장
                .build();
    }
}