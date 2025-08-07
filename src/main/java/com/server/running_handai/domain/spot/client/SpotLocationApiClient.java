package com.server.running_handai.domain.spot.client;

import com.server.running_handai.domain.spot.dto.SpotLocationApiResponseDto;
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
public class SpotLocationApiClient {
    private final WebClient webClient;

    @Value("${external.api.spot.base-url}")
    private String baseUrl;

    @Value("${external.api.spot.service-key}")
    private String serviceKey;

    @Value("${external.api.spot.radius}")
    private String radius;

    /**
     * [국문 관광정보] 위치기반 관광정보 조회 API를 요청합니다.
     *
     * @param pageNo 현재 페이지 번호
     * @param numOfRows 한 페이지 결과 수
     * @param arrange 정렬 구분 (E: 거리순, S: 대표 이미지가 반드시 있는 거리순)
     * @param lon 경도 (x)
     * @param lat 위도 (y)
     * @param contentTypeId 관광 타입 (12: 관광지, 39: 음식점)
     * @return SpotLocationResponseDto
     */
    public SpotLocationApiResponseDto fetchSpotLocationData(
            int pageNo,
            int numOfRows,
            String arrange,
            double lon,
            double lat,
            int contentTypeId
    ) {
        // URL 생성
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/locationBasedList2")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "runninghandai")
                .queryParam("_type", "Json")
                .queryParam("arrange", arrange)
                .queryParam("mapX", String.valueOf(lon))
                .queryParam("mapY", String.valueOf(lat))
                .queryParam("radius", radius)
                .queryParam("contentTypeId", String.valueOf(contentTypeId))
                .queryParam("serviceKey", serviceKey);

        URI uri = builder.build(true).toUri();

        // API 호출
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(SpotLocationApiResponseDto.class)
                .block();
    }
}