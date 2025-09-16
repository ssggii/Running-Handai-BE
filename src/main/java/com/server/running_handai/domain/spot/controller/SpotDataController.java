package com.server.running_handai.domain.spot.controller;

import com.server.running_handai.domain.spot.service.SpotDataService;
import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    @PostMapping("/sync-spots/date")
    public ResponseEntity<CommonResponse<?>> syncSpotsByDate(@RequestParam(required = false) String date) {
        String targetDate = date;
        if (targetDate == null) {
            // 호출 기준 전날 날짜 계산 (YYYYMMDD)
            targetDate = LocalDate.now()
                    .minusDays(1)
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        }
        spotDataService.syncSpotsByDate(targetDate);
        return ResponseEntity.ok().body(CommonResponse.success(ResponseCode.SUCCESS, null));
    }

    @PostMapping("/sync-spots/location")
    public ResponseEntity<CommonResponse<?>> syncSpotsByLocation() {
        spotDataService.syncSpotsByLocation();
        return ResponseEntity.ok().body(CommonResponse.success(ResponseCode.SUCCESS, null));
    }
}