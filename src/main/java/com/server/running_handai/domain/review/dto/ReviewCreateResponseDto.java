package com.server.running_handai.domain.review.dto;

import com.server.running_handai.domain.review.entity.Review;
import java.time.format.DateTimeFormatter;

public record ReviewCreateResponseDto(
    long reviewId,
    double stars,
    String contents,
    String writerNickname,
    String createdAt
) {
    public static ReviewCreateResponseDto from(Review review) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedCreatedAt = review.getCreatedAt().format(formatter);

        return new ReviewCreateResponseDto(
                review.getId(),
                review.getStars(),
                review.getContents(),
                review.getWriter().getNickname(),
                formattedCreatedAt
        );
    }
}
