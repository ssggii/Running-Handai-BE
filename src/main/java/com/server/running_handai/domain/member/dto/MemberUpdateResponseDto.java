package com.server.running_handai.domain.member.dto;

public record MemberUpdateResponseDto (
        Long memberId,
        String nickname
) {
    public static MemberUpdateResponseDto from(Long memberId, String nickname) {
        return new MemberUpdateResponseDto(memberId, nickname);
    }
}