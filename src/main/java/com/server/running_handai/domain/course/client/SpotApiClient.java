package com.server.running_handai.domain.course.client;

import com.server.running_handai.domain.course.dto.SpotApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpotApiClient {
    private final WebClient webClient;

    @Value("${external.api.spot.base-url}")
    private String baseUrl;

    @Value("${external.api.spot.service-key}")
    private String serviceKey;

    @Value("${external.api.spot.radius}")
    private String radius;

    /**
     * [국문 관광정보] 공통정보 조회 API를 요청합니다.
     *
     * @param contentId 장소 고유번호
     * @return SpotApiResponseDto
     */
    public SpotApiResponseDto fetchSpotData(String contentId) {
        // URL 생성
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/detailCommon2")
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "runninghandai")
                .queryParam("_type", "Json")
                .queryParam("contentId", contentId)
                .queryParam("serviceKey", serviceKey);

        URI uri = builder.build(true).toUri();
        log.info(String.valueOf(uri));

        // API 호출
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(SpotApiResponseDto.class)
                .block();
    }
}