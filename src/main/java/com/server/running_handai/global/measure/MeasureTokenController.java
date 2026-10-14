package com.server.running_handai.global.measure;

import com.server.running_handai.global.jwt.JwtProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인이 필요한 API를 로컬에서 확인할 때 쓰는 액세스 토큰을 발급합니다. (measure 프로필 전용)
 */
@Profile("measure")
@RestController
@RequestMapping("/measure/token")
@RequiredArgsConstructor
public class MeasureTokenController {

    private final JwtProvider jwtProvider;

    @PostMapping
    public Map<String, String> issue(@RequestParam long memberId) {
        return Map.of("accessToken", jwtProvider.createAccessToken(memberId));
    }
}
