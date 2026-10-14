package com.server.running_handai.global.measure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * 측정 API(/measure/**)와 지표(/metrics/**)를 인증 없이 허용합니다. (measure 프로필 전용)
 */
@Profile("measure")
@Configuration
public class MeasureSecurityConfig {

    @Bean
    public WebSecurityCustomizer measureWebSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/measure/**", "/metrics/**");
    }
}
