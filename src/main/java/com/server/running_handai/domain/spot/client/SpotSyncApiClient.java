package com.server.running_handai.domain.spot.client;

import com.server.running_handai.domain.spot.dto.SpotSyncApiResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpotSyncApiClient {
    private final WebClient webClient;

    @Value("${external.api.spot.base-url}")
    private String baseUrl;

    @Value("${external.api.spot.service-key}")
    private String serviceKey;

    /**
     * [국문 관광정보] 관광정보 동기화 목록 조회 API를 요청합니다.
     * 요청한 수정일을 기준으로 해당 날짜에 변경된 콘텐츠가 있는 경우 해당 콘텐츠의 정보를 응답합니다.
     *
     * @param areaCode 지역 코드
     * @return SpotSyncApiResponseDto
     */
    public SpotSyncApiResponseDto fetchSpotSyncData(int areaCode) {
        // 호출 기준 전날 날짜 계산 (YYYYMMDD)
        String modifiedDate = LocalDate.now()
                .minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // URL 생성
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/areaBasedSyncList2")
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "runninghandai")
                .queryParam("_type", "json")
                .queryParam("areaCode", String.valueOf(areaCode))
                .queryParam("modifiedtime", modifiedDate)
                .queryParam("serviceKey", serviceKey);

        URI uri = builder.build(true).toUri();

        // API 호출
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(SpotSyncApiResponseDto.class)
                .block();
    }
}
