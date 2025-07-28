package com.server.running_handai.domain.course.service;

import com.server.running_handai.global.response.ResponseCode;
import com.server.running_handai.global.response.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
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
     * @param variables 프롬프트에 바인딩할 변수
     * @return OpenAI API의 응답 결과 (문자열)
     */
    public String getOpenAiResponse(Resource promptResource, Map<String, Object> variables) {
        try {
            ChatClient chatClient = chatClientBuilder.build();
            Prompt prompt = createPrompt(promptResource, variables);

            ChatResponse response = chatClient
                    .prompt(prompt)
                    .call()
                    .chatResponse();

            // 실제 토큰 사용량 확인
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Usage usage = response.getMetadata().getUsage();
                log.info("[OpenAI 호출] 실제 사용된 토큰: token={}", usage.getPromptTokens());
            }

            return response.getResult()
                    .getOutput()
                    .getContent();

        } catch (Exception e) {
            log.error("[OpenAI 호출] OpenAI 응답 실패: message={}", e.getMessage());
            throw new BusinessException(ResponseCode.OPENAI_API_ERROR);
        }
    }

    /**
     * 프롬프트 템플릿과 프롬프트에 바인딩할 변수들을 입력받아 치환 완료된 OpenAI API 요청값을 기준으로 토큰 수를 계산합니다.
     *
     * @param promptResource 프롬프트 템플릿
     * @param variables 프롬프트에 바인딩할 변수
     * @return token 예상 토큰 수
     */
    public int calculateRequestToken(Resource promptResource, Map<String, Object> variables) {
        try {
            Prompt prompt = createPrompt(promptResource, variables);
            String requestPrompt = prompt.getInstructions().getFirst().getContent();

            // 예상 토큰값 계산
            TokenCountEstimator tokenCountEstimator = new JTokkitTokenCountEstimator();
            return tokenCountEstimator.estimate(requestPrompt);
        } catch (Exception e) {
            log.error("[OpenAI 호출] 프롬프트 템플릿 처리 실패: message={}", e.getMessage());
            throw new BusinessException(ResponseCode.OPENAI_API_ERROR);
        }
    }

    /**
     * 프롬프트 템플릿과 변수를 바인딩하여 Prompt 객체를 생성합니다.
     *
     * @param promptResource 프롬프트 템플릿
     * @param variables 프롬프트에 바인딩할 변수
     * @return 생성된 Prompt 객체
     */
    private Prompt createPrompt(Resource promptResource, Map<String, Object> variables) {
        PromptTemplate promptTemplate = new PromptTemplate(promptResource);
        return promptTemplate.create(variables);
    }
}
