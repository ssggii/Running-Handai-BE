package com.server.running_handai.global.measure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.server.running_handai.domain.course.service.FileService;
import com.server.running_handai.domain.course.service.KakaoMapService;
import com.server.running_handai.domain.course.service.OpenAiService;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * 외부 API(OpenAI, 카카오, S3)를 실제로 부르지 않고 지연만 재현하는 스텁입니다. (measure-stub 프로필 전용)
 * 지연 시간은 운영 로그 8건의 중앙값이 기본값입니다.
 */
@Profile("measure-stub")
@Configuration
public class ExternalApiStubConfig {

    private static final String OPENAI_RESPONSE = "MEDIUM|측정용 설명 1|측정용 설명 2|측정용 설명 3|측정용 설명 4|측정용 설명 5";
    private static final String KAKAO_ADDRESS = "{\"address\":{\"region_2depth_name\":\"해운대구\",\"region_3depth_name\":\"우동\"}}";

    @Bean
    @Primary
    public OpenAiService stubOpenAiService(ChatClient.Builder chatClientBuilder,
                                           @Value("${measure.stub.openai-delay-ms:4500}") long delayMs) {
        return new OpenAiService(chatClientBuilder) {
            @Override
            public String getOpenAiResponse(Resource promptResource, Map<String, Object> variables) {
                sleep(delayMs);
                return OPENAI_RESPONSE;
            }
        };
    }

    @Bean
    @Primary
    public KakaoMapService stubKakaoMapService(RestTemplate restTemplate, ObjectMapper objectMapper,
                                               @Value("${measure.stub.kakao-delay-ms:50}") long delayMs) throws Exception {
        JsonNode address = objectMapper.readTree(KAKAO_ADDRESS);
        return new KakaoMapService(restTemplate, objectMapper) {
            @Override
            public JsonNode getAddressFromCoordinate(double longitude, double latitude) {
                sleep(delayMs);
                return address;
            }
        };
    }

    @Bean
    @Primary
    public FileService stubFileService(S3Client s3Client,
                                       @Value("${measure.stub.s3-delay-ms:100}") long delayMs) {
        return new FileService(s3Client) {
            @Override
            public String uploadFile(MultipartFile multipartFile, String directory) {
                sleep(delayMs);
                return "https://stub.local/" + directory + "/" + UUID.randomUUID();
            }

            @Override
            public void deleteFile(String fileUrl) {
                sleep(delayMs);
            }
        };
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
