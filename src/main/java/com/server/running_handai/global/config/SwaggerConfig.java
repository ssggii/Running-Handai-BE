package com.server.running_handai.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    Server localServer = new Server().url("http://localhost:8080").description("로컬 테스트 서버");
    Server prodServer = new Server().url("http://43.202.125.162").description("운영 서버");

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("러닝한다이 API 명세서")
                .version("v1.0.0")
                .description("본 문서는 러닝한다이 API의 기능별 사용 방법과 명세를 정의합니다.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, prodServer));
    }

}
