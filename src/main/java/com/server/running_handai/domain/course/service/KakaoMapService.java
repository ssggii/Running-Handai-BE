package com.server.running_handai.domain.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoMapService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoApiKey;

    @Value("${kakao.address}")
    private String addressRequestUrl;

    @Value("${kakao.region-code}")
    private String regionCodeRequestUrl;

    public record AddressInfo(String districtName, String dongName) {}

    /**
     * 주어진 위도(latitude), 경도(longitude) 좌표로부터 카카오 지도 API를 통해 주소 정보를 조회합니다.
     *
     * @param longitude 경도 (x)
     * @param latitude 위도 (y)
     * @return 주소 정보가 담긴 JsonNode (성공 시 documents[0]), 없거나 파싱 실패시 null
     */
    public JsonNode getAddressFromCoordinate(double longitude, double latitude) {
        String requestUrl = addressRequestUrl
                + "?x=" + longitude
                + "&y=" + latitude
                + "&input_coord=WGS84"; // 좌표계 (기본값)

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            // documents 안에 도로명 주소(road_address)와 지번 주소(address)가 포함되어 응답
            JsonNode documents = root.path("documents");

            // 도로명 주소(road_address)는 좌표에 따라 반환되지 않을 수 있기 때문에 지번 주소(address)를 기준으로 함
            if (documents.isArray() && !documents.isEmpty()) {
                return documents.get(0);
            }

            log.warn("[카카오 지도 API 호출] 카카오 지도 API에서 주소 정보 없음: x={}, y={}", longitude, latitude);
            return null;

        } catch (Exception e) {
            log.error("[카카오 지도 API 호출] 카카오 지도 API 파싱 실패: x={}, y={}", longitude, latitude, e);
            return null;
        }
    }

    /**
     * 카카오 지도 API에서 가져온 주소 정보에서 구 단위, 동 단위를 추출합니다.
     *
     * @param jsonNode 주소 정보 JSON
     * @return districtName, dongName으로 구성된 AddressInfo, 없으면 null
     */
    public AddressInfo extractDistrictNameAndDongName(JsonNode jsonNode) {
        if (jsonNode == null) {
            return new AddressInfo(null, null);
        }

        String districtName = textToNull(jsonNode.path("address").path("region_2depth_name").asText());
        String dongName = textToNull(jsonNode.path("address").path("region_3depth_name").asText());

        return new AddressInfo(districtName, dongName);
    }

    /**
     * 주어진 위도(latitude), 경도(longitude) 좌표로부터 카카오 지도 API를 통해 행정구역 정보를 조회합니다.
     *
     * @param longitude 경도 (x)
     * @param latitude 위도 (y)
     * @return 행정구역 정보가 담긴 JsonNode (성공 시 documents[0]), 없거나 파싱 실패시 null
     */
    public JsonNode getRegionCodeFromCoordinate(double longitude, double latitude) {
        String requestUrl = regionCodeRequestUrl
                + "?x=" + longitude
                + "&y=" + latitude;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            // documents 안에 해당 좌표에 부합하는 행정동(H), 법정동(B) 행정구역 정보가 포함되어 응답
            JsonNode documents = root.path("documents");

            // 법으로 지정되어 행정동(H)보다 안정적인 법정동(B)을 기준으로 함
            if (documents.isArray() && !documents.isEmpty()) {
                return documents.get(0);
            }

            log.warn("[카카오 지도 API 호출] 카카오 지도 API에서 행정구역 정보 없음: x={}, y={}", longitude, latitude);
            return null;

        } catch (Exception e) {
            log.error("[카카오 지도 API 호출] 카카오 지도 API 파싱 실패: x={}, y={}", longitude, latitude, e);
            return null;
        }
    }

    /**
     * 카카오 지도 API에서 가져온 행정구역 정보에서 시도 단위를 추출합니다.
     *
     * @param jsonNode 행정구역 정보 JSON
     * @return provinceName, 없으면 null
     */
    public String extractProvinceName(JsonNode jsonNode) {
        if (jsonNode == null) {
            return null;
        }

        return textToNull(jsonNode.path("region_1depth_name").asText());
    }

    /**
     * 주어진 텍스트가 Null이거나 공백 문자인 경우 Null로 반환합니다.
     */
    private String textToNull(String text) {
        return (text == null || text.isBlank()) ? null : text;
    }
}