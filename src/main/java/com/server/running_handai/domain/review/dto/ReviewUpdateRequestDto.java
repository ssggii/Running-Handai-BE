package com.server.running_handai.domain.review.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewUpdateRequestDto(
        @Parameter(description = "별점 (0.5점 단위)")
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하이어야 합니다.")
        Double stars,

        @Parameter(description = "리뷰 내용 (최대 2000자)")
        @NotBlank(message = "리뷰 내용은 필수 항목입니다.")
        @Size(max = 2000, message = "리뷰 내용은 최대 2000자까지 작성할 수 있습니다.")
        String contents
) {
}
