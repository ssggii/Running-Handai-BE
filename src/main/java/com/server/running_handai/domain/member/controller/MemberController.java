package com.server.running_handai.domain.member.controller;

import com.server.running_handai.domain.member.dto.MemberInfoDto;
import com.server.running_handai.global.oauth.CustomOAuth2User;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.domain.member.dto.TokenRequestDto;
import com.server.running_handai.domain.member.dto.TokenResponseDto;
import com.server.running_handai.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "Member", description = "회원 관련 API")
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "토큰 재발급",
            description = "리프래쉬 토큰을 통해 만료된 액세스 토큰을 재발급합니다. 인증에 사용된 리프래시 토큰 역시 액세스 토큰과 함께 재발급됩니다. 로그인은 /oauth2/authorization/{provider}로 요청해주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "실패 (유효하지 않은 토큰)"),
            @ApiResponse(responseCode = "404", description = "실패 (찾을 수 없는 리프래시 토큰)")
    })
    @PostMapping("/oauth/token")
    public ResponseEntity<CommonResponse<TokenResponseDto>> createToken(@RequestBody TokenRequestDto tokenRequestDto) {
        TokenResponseDto tokenResponseDto =  memberService.createToken(tokenRequestDto);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, tokenResponseDto));
    }

    @Operation(summary = "내 정보 조회", description = "회원의 닉네임과 이메일을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "실패 (유효하지 않은 토큰)"),
    })
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<MemberInfoDto>> getMyInfo(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        MemberInfoDto memberInfo = memberService.getMemberInfo(customOAuth2User.getMember().getId());
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, memberInfo));
    }

}
