package com.server.running_handai.global.oauth.userInfo;

import com.server.running_handai.member.entity.Provider;

public interface OAuth2UserInfo {
    String getProviderId();
    Provider getProvider();
    String getEmail();
}
