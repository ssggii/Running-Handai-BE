package com.server.running_handai.global.oauth.userInfo;

import com.server.running_handai.domain.member.entity.Provider;

import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo{
    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderId() {
        return attributes.get("sub").toString();
    }

    @Override
    public Provider getProvider() {
        return Provider.GOOGLE;
    }

    @Override
    public String getEmail() {
        return attributes.get("email").toString();
    }
}