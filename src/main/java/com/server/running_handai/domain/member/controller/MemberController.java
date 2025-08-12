package com.server.running_handai.domain.member.controller;

import com.server.running_handai.domain.member.dto.MemberUpdateRequestDto;
import com.server.running_handai.domain.member.dto.MemberUpdateResponseDto;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "Member", description = "회원 관련 API")
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "토큰 재발급",
            description = "리프래쉬 토큰을 통해 만료된 액세스 토큰을 재발급합니다. 인증에 사용된 리프래시 토큰 역시 액세스 토큰과 함께 재발급됩니다. " +
                    "로그인은 /oauth2/authorization/{provider}로 요청해주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 - SUCCESS"),
            @ApiResponse(responseCode = "401", description = "실패 (유효하지 않은 토큰) - INVALID_REFRESH_TOKEN"),
            @ApiResponse(responseCode = "404", description = "실패 (찾을 수 없는 리프래시 토큰) - REFRESH_TOKEN_NOT_FOUND")
    })
    @PostMapping("/oauth/token")
    public ResponseEntity<CommonResponse<TokenResponseDto>> createToken(@RequestBody TokenRequestDto tokenRequestDto) {
        TokenResponseDto tokenResponseDto =  memberService.createToken(tokenRequestDto);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, tokenResponseDto));
    }

    @Operation(summary = "닉네임 중복 여부 조회",
            description = "사용자가 수정하려는 닉네임이 중복이 아닌 경우 true, 중복인 경우 false를 응답합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 - SUCCESS"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 필요 - UNAUTHORIZED_ACCESS"),
            @ApiResponse(responseCode = "404", description = "실패 (찾을 수 없는 사용자) - MEMBER_NOT_FOUND"),
            @ApiResponse(responseCode = "400", description = "실패 (글자수가 2글자 미만, 10글자 초과) - INVALID_NICKNAME_LENGTH"),
            @ApiResponse(responseCode = "400", description = "실패 (한글, 영문, 숫자 외의 문자가 존재) - INVALID_NICKNAME_FORMAT"),
            @ApiResponse(responseCode = "400", description = "실패 (현재 사용 중인 닉네임과 동일) - SAME_AS_CURRENT_NICKNAME"),
    })
    @GetMapping("/nickname")
    public ResponseEntity<CommonResponse<Boolean>> checkNicknameDuplicate(
            @RequestParam("value") String nickname,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        log.info("[닉네임 중복 여부 조회] memberId: {} nickname: {}", memberId, nickname);
        Boolean result = memberService.checkNicknameDuplicate(memberId, nickname);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, result));
    }

    @Operation(summary = "내 정보 수정",
            description = "내 정보를 수정합니다. 현재는 닉네임 수정만 제공합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 - SUCCESS"),
            @ApiResponse(responseCode = "401", description = "토큰 인증 필요 - UNAUTHORIZED_ACCESS"),
            @ApiResponse(responseCode = "404", description = "실패 (찾을 수 없는 사용자) - MEMBER_NOT_FOUND"),
            @ApiResponse(responseCode = "400", description = "실패 (글자수가 2글자 미만, 10글자 초과) - INVALID_NICKNAME_LENGTH"),
            @ApiResponse(responseCode = "400", description = "실패 (한글, 영문, 숫자 외의 문자가 존재) - INVALID_NICKNAME_FORMAT"),
            @ApiResponse(responseCode = "400", description = "실패 (현재 사용 중인 닉네임과 동일) - SAME_AS_CURRENT_NICKNAME"),
            @ApiResponse(responseCode = "409", description = "실패 (중복된 닉네임) - DUPLICATE_NICKNAME"),
    })
    @PatchMapping("/me")
    public ResponseEntity<CommonResponse<MemberUpdateResponseDto>> updateMemberInfo(
            @RequestBody MemberUpdateRequestDto memberUpdateRequestDto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        Long memberId = customOAuth2User.getMember().getId();
        log.info("[내 정보 수정] memberId: {}", memberId);
        MemberUpdateResponseDto memberUpdateResponseDto = memberService.updateMemberInfo(memberId, memberUpdateRequestDto);
        return ResponseEntity.ok(CommonResponse.success(ResponseCode.SUCCESS, memberUpdateResponseDto));
    }
}
