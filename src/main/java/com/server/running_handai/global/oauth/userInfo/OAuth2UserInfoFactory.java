package com.server.running_handai.global.oauth.userInfo;

import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "kakao" -> new KakaoUserInfo(attributes);
            case "google" -> new GoogleUserInfo(attributes);
            case "naver" -> new NaverUserInfo(attributes);
            default -> throw new BusinessException(ResponseCode.INVALID_PROVIDER);
        };
    }
}