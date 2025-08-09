package com.server.running_handai.domain.spot.controller;

import com.server.running_handai.domain.spot.service.SpotDataService;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/courses")
@RequiredArgsConstructor
public class SpotDataController {
    private final SpotDataService spotDataService;

    @PutMapping("/{courseId}/spots")
    public ResponseEntity<CommonResponse<?>> updateSpots(@PathVariable Long courseId) {
        spotDataService.updateSpots(courseId);
        return ResponseEntity.ok().body(CommonResponse.success(ResponseCode.SUCCESS, null));
    }
}