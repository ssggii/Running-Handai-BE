package com.server.running_handai.course.entity;

import static com.server.running_handai.course.entity.AreaCategory.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Area {
    HAEUN_GWANGAN("해운/광안", SEA, List.of("해운대구", "수영구")),
    SONGJEONG_GIJANG("송정/기장", SEA, List.of("기장군", "송정동")),
    SEOMYEON_DONGNAE("서면/동래", DOWNTOWN, List.of("부산진구", "동래구", "연제구")),
    WONDOSIM("원도심/영도", DOWNTOWN, List.of("중구", "동구", "서구", "영도구")),
    SOUTHERN_COAST("남부해안", SEA, List.of("남구")),
    WESTERN_NAKDONGRIVER("서부/낙동강", RIVERSIDE, List.of("사상구", "강서구", "사하구")),
    NORTHERN_BUSAN("북부산", RIVERSIDE, List.of("금정구", "북구"));

    private final String description;
    private final AreaCategory category;
    private final List<String> subRegions;

    /**
     * 하위 지역명(String)을 받아 해당하는 Area enum을 찾아 반환합니다.
     *
     * @param subRegionName (ex. "해운대구", "중구")
     * @return 일치하는 Area enum을 Optional로 감싸서 반환, 없으면 Optional.empty()
     */
    public static Optional<Area> findBySubRegion(String subRegionName) {
        return Arrays.stream(Area.values())
                .filter(area -> area.getSubRegions().contains(subRegionName))
                .findFirst();
    }
}
