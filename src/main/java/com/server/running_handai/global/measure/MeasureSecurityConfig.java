package com.server.running_handai.global.measure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * 측정 전용 API(/measure/**)를 인증 없이 호출할 수 있게 합니다. (measure 프로필에서만 등록, 운영 보안 설정은 그대로)
 */
@Profile("measure")
@Configuration
public class MeasureSecurityConfig {

    @Bean
    public WebSecurityCustomizer measureWebSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/measure/**");
    }
}
