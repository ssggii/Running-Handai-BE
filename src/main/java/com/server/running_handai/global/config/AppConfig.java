package com.server.running_handai.global.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public GeometryFactory geometryFactory() {
        return new GeometryFactory();
    }
}
