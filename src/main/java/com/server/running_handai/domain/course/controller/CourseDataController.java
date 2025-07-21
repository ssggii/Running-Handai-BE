package com.server.running_handai.domain.course.controller;

import com.server.running_handai.domain.course.dto.GpxCourseRequestDto;
import com.server.running_handai.domain.course.service.CourseDataService;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
    public ResponseEntity<CommonResponse<?>> updateRoadCondition(@PathVariable Long courseId) {
        courseDataService.updateRoadCondition(courseId);
        return ResponseEntity.ok().body(CommonResponse.success(ResponseCode.SUCCESS, null));
    }

    @PutMapping(value = "/{courseId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<?>> updateCourseImage(@PathVariable Long courseId,
                                                               @RequestParam MultipartFile courseImageFile) throws IOException {
        courseDataService.updateCourseImage(courseId, courseImageFile);
        return ResponseEntity.ok().body(CommonResponse.success(ResponseCode.SUCCESS, null));
    }

    @PostMapping(value = "/gpx", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<?>> createCourseToGpx(@RequestPart("courseInfo") GpxCourseRequestDto gpxCourseRequestDto,
                                                               @RequestParam("courseGpxFile") MultipartFile courseGpxFile) throws IOException {
        courseDataService.createCourseToGpx(gpxCourseRequestDto, courseGpxFile);
        return ResponseEntity.ok().body(CommonResponse.success(ResponseCode.SUCCESS, null));
    }
}
