package com.server.running_handai.global.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

@Configuration
public class AppConfig {

    @Bean
    public GeometryFactory geometryFactory() {
        // SRID 4326은 위도, 경도를 사용하는 표준 WGS 84 좌표계를 의미
        return new GeometryFactory(new PrecisionModel(), 4326);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // OpenAI API 관련에서 발생하는 알 수 없는 필드가 있다는 예외를 무시하기 위함
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    public XmlMapper xmlMapper() {
        return new XmlMapper();
    }
}
