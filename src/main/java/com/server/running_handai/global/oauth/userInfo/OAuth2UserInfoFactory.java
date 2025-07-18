package com.server.running_handai.global.oauth.userInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        switch (registrationId.toLowerCase()) {
            // todo: Google, Naver 구현 후 추가
            case "kakao":
                return new KakaoUserInfo(attributes);
            default:
                throw new IllegalArgumentException("지원하지 않는 OAuth2 Provider입니다: " + registrationId);
        }
    }
}
