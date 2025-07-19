package com.server.running_handai.global.oauth.userInfo;

import com.server.running_handai.domain.member.entity.Provider;

import java.util.Map;

public class NaverUserInfo implements OAuth2UserInfo{
    private final Map<String, Object> attributes;

    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return response.get("id").toString();
    }

    @Override
    public Provider getProvider() {
        return Provider.NAVER;
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return response.get("email").toString();
    }
}