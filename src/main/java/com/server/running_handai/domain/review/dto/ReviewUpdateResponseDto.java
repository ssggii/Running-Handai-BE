package com.server.running_handai.domain.review.dto;

import com.server.running_handai.domain.review.entity.Review;
import java.time.format.DateTimeFormatter;

public record ReviewUpdateResponseDto(
        long reviewId,
        double stars,
        String contents,
        String updatedAt
) {
    public static ReviewUpdateResponseDto from(Review review) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedUpdatedAt = review.getUpdatedAt().format(formatter);
        return new ReviewUpdateResponseDto(
                review.getId(),
                review.getStars(),
                review.getContents(),
                formattedUpdatedAt
        );
    }
}
