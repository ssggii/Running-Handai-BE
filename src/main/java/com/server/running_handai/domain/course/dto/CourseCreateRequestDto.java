package com.server.running_handai.domain.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record CourseCreateRequestDto(
        @NotBlank(message = "시작 지점 이름은 필수입니다.")
        @Size(max = 20, message = "시작 지점 이름은 20자를 초과할 수 없습니다.")
        String startPointName,

        @NotBlank(message = "종료 지점 이름은 필수입니다.")
        @Size(max = 20, message = "종료 지점 이름은 20자를 초과할 수 없습니다.")
        String endPointName,

        @NotNull(message = "GPX 파일은 필수입니다.")
        MultipartFile gpxFile,

        @NotNull(message = "썸네일 이미지는 필수입니다.")
        MultipartFile thumbnailImage
) {
}
