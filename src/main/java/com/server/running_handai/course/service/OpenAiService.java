package com.server.running_handai.course.service;

import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class OpenAiService {
    private final ChatClient.Builder chatClientBuilder;

    public OpenAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    /**
     * 지정된 프롬프트와 프롬프트에 필요한 변수를 바인딩해 OpenAI API를 호출하고,
     * 응답 결과를 문자열로 반환합니다.
     *
     * @param promptResource 프롬프트 템플릿 (예: classpath:prompt/save-road-condition.st)
     * @param variables  프롬프트에 바인딩할 변수
     * @return OpenAI API의 응답 결과 (문자열)
     */
    public String getOpenAiResponse(Resource promptResource, Map<String, Object> variables) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            PromptTemplate promptTemplate = new PromptTemplate(promptResource);
            Prompt prompt = promptTemplate.create(variables);

            return chatClient.prompt(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[OpenAI 호출] OpenAI 응답 실패", e);
            throw new BusinessException(ResponseCode.OPENAI_API_ERROR);
        }
    }
}
