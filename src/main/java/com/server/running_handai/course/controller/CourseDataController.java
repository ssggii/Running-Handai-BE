package com.server.running_handai.course.controller;

import com.server.running_handai.course.service.CourseDataService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class CourseDataController {

    private final CourseDataService courseDataService;

    @PostMapping("/sync-courses")
    public ResponseEntity<Map<String, String>> synchronizeCourses() {
        courseDataService.synchronizeCourseData();
        String message = "코스 데이터 동기화 작업이 시작되었습니다. 서버 로그를 통해 진행 상황을 확인하세요.";
        Map<String, String> responseBody = Map.of("message", message);
        return ResponseEntity.accepted().body(responseBody);
    }

    @PostMapping("/sync-trackpoints")
    public ResponseEntity<Map<String, String>> saveTrackPoints() {
        courseDataService.syncAllCoursesWithEmptyTrackPoints();
        String message = "트랙포인트 동기화 작업이 시작되었습니다. 서버 로그를 통해 진행 상황을 확인하세요.";
        Map<String, String> responseBody = Map.of("message", message);
        return ResponseEntity.accepted().body(responseBody);
    }
}
