package com.server.running_handai.domain.course.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum Category {
    NATURE("자연관광지", List.of("A01010100", "A01010200", "A01010300", "A01010400", "A01010500", "A01010600", "A01010700", "A01010800", "A01010900", "A01011000", "A01020100", "A01020200")),
    HISTORY("역사관광지", List.of("A02010100", "A02010200", "A02010300", "A02010400", "A02010500", "A02010600", "A02010700", "A02010800", "A02010900", "A02011000")),
    RECREATION("휴양관광지", List.of("A02020200", "A02020300", "A02020400", "A02020500", "A02020600", "A02020700", "A02020800")),
    EXPERIENCE("체험관광지", List.of("A02030100", "A02030200", "A02030300", "A02030400", "A02030600")),
    INDUSTRIAL("산업관광지", List.of("A02040400", "A02040600", "A02040800", "A02040900", "A02041000")),
    ARCHITECTURE("건축조형물", List.of("A02050100", "A02050200", "A02050300", "A02050400", "A02050500", "A02050600")),
    KOREAN_FOOD("한식", List.of("A05020100")),
    WESTERN_FOOD("서양식", List.of("A05020200")),
    JAPANESE_FOOD("일식", List.of("A05020300")),
    CHINESE_FOOD("중식", List.of("A05020400")),
    GLOBAL_FOOD("이색음식점", List.of("A05020700")),
    CAFE("카페", List.of("A05020900")),
    CLUB("클럽", List.of("A05021000"));

    private final String description;
    private final List<String> categoryNumber;

    /**
     * 주어진 Category Number를 포함하는 Category Enum을 찾아 반환합니다.
     *
     * @param categoryNumber 카테고리 번호 (예: "A01010100")
     * @return 일치하는 Category Enum을 Optional로 감싸서 반환, 없으면 Optional.empty()
     */
    public static Optional<Category> findByCategoryNumber(String categoryNumber) {
        return Arrays.stream(Category.values())
                .filter(category -> category.getCategoryNumber().contains(categoryNumber))
                .findFirst();
    }
}
