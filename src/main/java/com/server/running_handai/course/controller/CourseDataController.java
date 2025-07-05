package com.server.running_handai.course.controller;

import com.server.running_handai.course.dto.CourseImageResponseDto;
import com.server.running_handai.course.dto.RoadConditionResponseDto;
import com.server.running_handai.course.service.CourseDataService;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class CourseDataController {

    private final CourseDataService courseDataService;

    @PostMapping("/sync-courses")
    public ResponseEntity<CommonResponse<?>> synchronizeCourses() {
        courseDataService.synchronizeCourseData();
        return ResponseEntity.accepted().body(CommonResponse.success(ResponseCode.SUCCESS_COURSE_SYNC_ACCEPTED, null));
    }

    @PutMapping("/{courseId}/condition")
    public ResponseEntity<RoadConditionResponseDto> updateRoadCondition(@PathVariable Long courseId) {
        return new ResponseEntity<>(courseDataService.updateRoadCondition(courseId), HttpStatus.OK);
    }

    @PutMapping(value = "/{courseId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseImageResponseDto> updateCourseImage(@PathVariable Long courseId, @RequestParam MultipartFile courseImageFile) {
        return new ResponseEntity<>(courseDataService.updateCourseImage(courseId, courseImageFile), HttpStatus.OK);
    }
}
