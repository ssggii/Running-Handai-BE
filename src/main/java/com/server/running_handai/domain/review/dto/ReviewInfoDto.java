package com.server.running_handai.domain.review.dto;

import com.server.running_handai.domain.review.entity.Review;
import java.time.format.DateTimeFormatter;

public record ReviewInfoDto(
        long reviewId,
        double stars,
        String contents,
        String writerNickname,
        boolean isMyReview,
        String createdAt
) {
    public static ReviewInfoDto from(Review review, boolean isMyReview) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedCreatedAt = review.getCreatedAt().format(formatter);

        return new ReviewInfoDto(
                review.getId(),
                review.getStars(),
                review.getContents(),
                review.getWriter().getNickname(),
                isMyReview,
                formattedCreatedAt
        );
    }
}
