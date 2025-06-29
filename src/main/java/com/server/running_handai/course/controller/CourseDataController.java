package com.server.running_handai.course.controller;

import com.server.running_handai.course.service.CourseDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class CourseDataController {

    private final CourseDataService courseDataService;

    @PostMapping("/synchronize")
    public ResponseEntity<String> synchronizeCourses() {
        courseDataService.synchronizeCourseData();
        return ResponseEntity.accepted()
                .body("Course data synchronization has been initiated in the background.");
    }
}
