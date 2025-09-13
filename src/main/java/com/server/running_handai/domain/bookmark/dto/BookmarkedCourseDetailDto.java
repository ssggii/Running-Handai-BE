package com.server.running_handai.domain.bookmark.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record BookmarkedCourseDetailDto(
        int totalPages, // 전체 페이지 수
        long totalElements, // 전체 코스 수
        boolean isLastPage, // 마지막 페이지 여부
        List<BookmarkedCourseInfoDto> courses
) {
    public static BookmarkedCourseDetailDto from(Page<BookmarkedCourseInfoDto> page) {
        return new BookmarkedCourseDetailDto(
                page.getTotalPages(),
                page.getTotalElements(),
                page.isLast(),
                page.getContent()
        );
    }
}
