package com.server.running_handai.member.dto;

import lombok.Data;

@Data
public class TokenResponse {
    private String accessToken;

    public TokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
