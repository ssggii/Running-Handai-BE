package com.server.running_handai.domain.course.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CoordinateListDto(
        @NotNull(message = "좌표 배열은 null일 수 없습니다.")
        @Size(min = 2, message = "좌표 배열은 시작점과 도착점을 포함하여 최소 2개 이상이어야 합니다.")
        List<CoordinateDto> coordinateDtoList
) {
    public record CoordinateDto(double latitude, double longitude) {}
}