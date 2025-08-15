package com.server.running_handai.domain.member.dto;

import com.server.running_handai.domain.member.entity.Member;

public record MemberInfoDto(
        String nickname,
        String email
) {
    public static MemberInfoDto from(Member member) {
        return new MemberInfoDto(member.getNickname(), member.getEmail());
    }
}
