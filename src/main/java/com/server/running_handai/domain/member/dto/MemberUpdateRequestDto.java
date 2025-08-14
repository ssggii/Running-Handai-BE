package com.server.running_handai.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record MemberUpdateRequestDto (
        @NotBlank
        String nickname
) {
}
