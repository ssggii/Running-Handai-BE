package com.server.running_handai.global.oauth;

import com.server.running_handai.global.oauth.userInfo.OAuth2UserInfo;
import com.server.running_handai.global.oauth.userInfo.OAuth2UserInfoFactory;
import com.server.running_handai.domain.member.entity.Member;
import com.server.running_handai.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserService extends DefaultOAuth2UserService {
    private final MemberService memberService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        log.info("[OAuth2 로그인] 시작 - Provider: {}", registrationId);

        try {
            OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

            // Provider에 따라 사용자 정보 파싱
            OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
            Member member = memberService.createOrFindMember(oAuth2UserInfo);
            log.info("[OAuth2 로그인] 성공 - 사용자 ID: {}, Provider: {}", member.getId(), registrationId);

            // 반환하여 Security Context에 Authentication 객체 저장
            return new CustomOAuth2User(member, oAuth2User.getAttributes());
        } catch (Exception e) {
            log.error("[OAuth2 로그인] 실패 - Provider: {}, 오류: {}", registrationId, e.getMessage(), e);
            throw e;
        }
    }
}
