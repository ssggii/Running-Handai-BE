package com.server.running_handai.domain.course.dto;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record CourseUpdateRequestDto(
        @Size(max = 20, message = "시작 지점 이름은 20자를 초과할 수 없습니다.") String startPointName,
        @Size(max = 20, message = "종료 지점 이름은 20자를 초과할 수 없습니다.") String endPointName,
        MultipartFile thumbnailImage
) {
}
