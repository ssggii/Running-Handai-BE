package com.server.running_handai.course.service;

import com.server.running_handai.global.response.exception.BusinessException;
import io.swagger.v3.core.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static com.server.running_handai.global.response.ResponseCode.ADDRESS_PARSE_FAILED;

@Slf4j
@Service
public class KakaoAddressService {
    @Value("${KAKAO_API_KEY}")
    private String kakaoApiKey;

    /** kakao 지도 API 호출해서 위도, 경도의 주소 가져오기 */
    public JsonNode getAddressFromCoordinate(double longitude, double latitude) {
        String requestUrl = "https://dapi.kakao.com/v2/local/geo/coord2address.json"
                + "?x=" + longitude
                + "&y=" + latitude
                + "&input_coord=WGS84"; // 좌표계 (기본값)

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                entity,
                String.class
        );

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response.getBody());

            // documents 안에 도로명 주소(road_address)와 지번 주소(address)가 포함되어 응답
            JsonNode documents = root.path("documents");

            if (documents.isArray() && !documents.isEmpty()) {
                return documents.get(0);
            }
        } catch (Exception e) {
            log.error("카카오 지도 API에서 주소 파싱을 실패하였습니다.");
            throw new BusinessException(ADDRESS_PARSE_FAILED);
        }

        // 해당하는 값이 없으면 null로 반환
        return null;
    }
}