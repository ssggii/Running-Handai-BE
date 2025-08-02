package com.server.running_handai.domain.review.dto;

import com.server.running_handai.domain.review.entity.Review;
import java.time.LocalDateTime;

public record ReviewUpdateResponseDto(
        long reviewId,
        double stars,
        String contents,
        LocalDateTime updatedAt
) {
    public static ReviewUpdateResponseDto from(Review review) {
        return new ReviewUpdateResponseDto(
                review.getId(),
                review.getStars(),
                review.getContents(),
                review.getUpdatedAt()
        );
    }
}
