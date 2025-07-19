package com.server.running_handai.domain.course.client;

import com.server.running_handai.domain.course.dto.DurunubiApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;


@Component
@RequiredArgsConstructor
public class DurunubiApiClient {

    private final WebClient webClient;

    @Value("${external.api.durunubi.base-url}")
    private String baseUrl;

    @Value("${external.api.durunubi.service-key}")
    private String serviceKey;

    public DurunubiApiResponseDto fetchCourseData(int pageNo, int numOfRows) {
        // URL 생성
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/courseList")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "runninghandai")
                .queryParam("serviceKey", serviceKey)
                .queryParam("_type", "Json")
                .queryParam("brdDiv", "DNWW"); // 걷기용 코스만 조회

        // API 호출
        return webClient.get()
                .uri(builder.build(true).toUri())
                .retrieve()
                .bodyToMono(DurunubiApiResponseDto.class) // DTO로 변환
                .block();
    }

}
