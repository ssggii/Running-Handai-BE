package com.server.running_handai.course.controller;

import com.server.running_handai.course.dto.RoadConditionResponseDto;
import com.server.running_handai.course.service.CourseDataService;
import com.server.running_handai.global.response.ApiResponse;
import com.server.running_handai.global.response.ResponseCode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class CourseDataController {

    private final CourseDataService courseDataService;

    @PostMapping("/sync-courses")
    public ResponseEntity<ApiResponse<?>> synchronizeCourses() {
        courseDataService.synchronizeCourseData();
        return ResponseEntity.accepted().body(ApiResponse.success(ResponseCode.SUCCESS_COURSE_SYNC_ACCEPTED, null));
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<RoadConditionResponseDto> updateRoadCondition(@PathVariable Long courseId) {
        return new ResponseEntity<>(courseDataService.updateRoadCondition(courseId), HttpStatus.OK);
    }
}
