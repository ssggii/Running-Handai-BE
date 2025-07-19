package com.server.running_handai.domain.member.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Provider {
    GOOGLE("구글"),
    NAVER("네이버"),
    KAKAO("카카오");

    private final String description;
}
