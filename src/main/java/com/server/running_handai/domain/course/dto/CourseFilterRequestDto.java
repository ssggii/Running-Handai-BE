package com.server.running_handai.domain.course.dto;

import static com.server.running_handai.global.response.ResponseCode.*;

import com.server.running_handai.domain.course.entity.Area;
import com.server.running_handai.domain.course.entity.CourseFilter;
import com.server.running_handai.domain.course.entity.Theme;
import com.server.running_handai.global.response.exception.BusinessException;
import io.swagger.v3.oas.annotations.Parameter;

public record CourseFilterRequestDto(
        @Parameter(description = "코스 필터링 옵션", required = true)
        CourseFilter filter,

        @Parameter(description = "사용자 현위치의 위도", required = true, example = "35.10")
        Double lat,

        @Parameter(description = "사용자 현위치의 경도", required = true, example = "129.12")
        Double lon,

        @Parameter(description = "지역 필터링 시 사용할 지역 코드")
        Area area,

        @Parameter(description = "테마 필터링 시 사용할 테마 코드")
        Theme theme
) {
    public CourseFilterRequestDto {
        if (filter == null) {
            throw new BusinessException(INVALID_COURSE_FILTER_TYPE);
        }
        if (lat == null || lon == null) {
            throw new BusinessException(INVALID_USER_POINT);
        }
    }
}
