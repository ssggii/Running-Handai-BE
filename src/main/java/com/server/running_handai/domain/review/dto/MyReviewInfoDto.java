package com.server.running_handai.domain.review.dto;

import com.server.running_handai.domain.course.entity.Course;
import com.server.running_handai.domain.review.entity.Review;
import java.time.format.DateTimeFormatter;

public record MyReviewInfoDto(
        long reviewId,
        long courseId,
        String courseName,
        String thumbnailUrl,
        String area,
        double distance,
        int duration,
        int maxElevation,
        double stars,
        String contents,
        String createdAt
) {
    public static MyReviewInfoDto from(Review review) {
        Course course = review.getCourse();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedCreatedAt = review.getCreatedAt().format(formatter);

        return new MyReviewInfoDto(
                review.getId(),
                course.getId(),
                course.getName(),
                (course.getCourseImage() != null) ? course.getCourseImage().getImgUrl() : null,
                course.getArea().name(),
                course.getDistance(),
                course.getDuration(),
                (int) course.getMaxElevation().doubleValue(),
                review.getStars(),
                review.getContents(),
                formattedCreatedAt
        );
    }
}
