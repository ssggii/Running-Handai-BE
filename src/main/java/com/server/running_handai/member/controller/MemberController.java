package com.server.running_handai.member.controller;

import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.member.dto.TokenRequest;
import com.server.running_handai.member.dto.TokenResponse;
import com.server.running_handai.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "Member", description = "회원 관련 API")
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "액세스 토큰 재발급",
            description = "리프래쉬 토큰을 통해 만료된 액세스 토큰을 재발급합니다. 로그인은 /oauth2/authorization/{provider}로 요청해주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "실패 (유효하지 않은 토큰)"),
            @ApiResponse(responseCode = "404", description = "실패 (찾을 수 없는 리프래시 토큰)")
    })
    @PostMapping("/oauth/token")
    public ResponseEntity<CommonResponse<TokenResponse>> createAccessToken(@RequestBody TokenRequest tokenRequest) {
        TokenResponse tokenResponse =  memberService.createAccessToken(tokenRequest);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS,tokenResponse));
    }
}
