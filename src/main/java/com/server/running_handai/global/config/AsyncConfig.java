package com.server.running_handai.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    // 코스 동기화용 스레드 풀
    @Bean(name = "syncCourseTaskExecutor")
    public Executor syncCourseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); // 기본 스레드 수
        executor.setMaxPoolSize(5); // 최대 스레드 수
        executor.setQueueCapacity(100); // 대기 큐 사이즈
        executor.setThreadNamePrefix("CourseSync-");
        executor.initialize();
        return executor;
    }

    // GPX 파싱 전용 스레드 풀
    @Bean(name = "gpxTaskExecutor")
    public Executor gpxTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("SaveTrackPoints-");
        executor.initialize();
        return executor;
    }

}
