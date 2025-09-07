package com.server.running_handai.global.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum SortBy {
    OLDEST("오래된순", Sort.by("created_at").ascending()),
    SHORT("거리짧은순", Sort.by("distance").ascending()),
    LONG("거리긴순", Sort.by("distance").descending()),
    LATEST("최신순", Sort.by("created_at").descending());

    private final String description;
    private final Sort sort;

    /**
     * 주어진 문자열을 받아 해당하는 SortBy Enum의 Sort 객체를 반환합니다.
     *
     * @param sortBy 정렬 기준을 나타내는 문자열
     * @return 해당하는 SortBy Enum 멤버의 Sort 객체, 없으면 SortBy.LATEST의 Sort 객체
     */
    public static Sort findBySort(String sortBy) {
        if (sortBy == null) {
            return SortBy.LATEST.getSort();
        }

        return Arrays.stream(SortBy.values())
                .filter(s -> s.name().equalsIgnoreCase(sortBy))
                .map(SortBy::getSort)
                .findFirst()
                .orElse(SortBy.LATEST.getSort());
    }
}
