package com.server.running_handai.global.response.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 존재하지_않는_리소스를_요청하면_404를_응답한다() throws Exception {
        // 인증 없이 접근 가능한 경로 아래의 없는 리소스는 DispatcherServlet까지 도달해 NoResourceFoundException이 발생한다
        mockMvc.perform(get("/api/courses/1/scripts/setup.php"))
                .andExpect(status().isNotFound());
    }
}
