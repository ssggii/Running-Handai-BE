package com.server.running_handai.domain.review.dto;

import com.server.running_handai.domain.review.entity.Review;
import java.time.LocalDateTime;

public record ReviewInfoDto(
    long reviewId,
    double stars,
    String contents,
    String writerNickname,
    LocalDateTime createdAt
) {
    public static ReviewInfoDto from(Review review) {
        return new ReviewInfoDto(
                review.getId(),
                review.getStars(),
                review.getContents(),
                review.getWriter().getNickname(),
                review.getCreatedAt()
        );
    }
}
