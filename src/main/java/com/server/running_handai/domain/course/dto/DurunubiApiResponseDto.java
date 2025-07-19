package com.server.running_handai.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Hidden
@Getter
@ToString
@NoArgsConstructor
public class DurunubiApiResponseDto {

    @JsonProperty("response")
    private Response response;

    @Getter
    @ToString
    @NoArgsConstructor
    public static class Response {
        @JsonProperty("header")
        private Header header;

        @JsonProperty("body")
        private Body body;
    }

    @Getter
    @ToString
    @NoArgsConstructor
    public static class Header {
        @JsonProperty("resultCode")
        private String resultCode;

        @JsonProperty("resultMsg")
        private String resultMsg;
    }

    @Getter
    @ToString
    @NoArgsConstructor
    public static class Body {
        @JsonProperty("items")
        private Items items;

        @JsonProperty("numOfRows")
        private int numOfRows;

        @JsonProperty("pageNo")
        private int pageNo;

        @JsonProperty("totalCount")
        private int totalCount;
    }

    @Getter
    @ToString
    @NoArgsConstructor
    public static class Items {
        @JsonProperty("item")
        private List<Item> itemList;
    }

    /**
     * 개별 코스 정보
     */
    @Getter
    @ToString
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        @JsonProperty("crsIdx")
        private String courseIndex; // 코스 고유번호 (Course.externalId)

        @JsonProperty("crsKorNm")
        private String courseName; // 코스 명 (Course.name)

        @JsonProperty("crsDstnc")
        private String courseDistance; // 코스 길이 (Course.distance)

        @JsonProperty("crsTotlRqrmHour")
        private String totalRequiredTime; // 소요 시간 (Course.duration)

        @JsonProperty("crsLevel")
        private String courseLevel; // 난이도 (Course.level)

        @JsonProperty("crsTourInfo")
        private String tourInfo; // 관광포인트 (Course.tourPoint)

        @JsonProperty("sigun")
        private String sigun; // 행정구역 (Course.area)

        @JsonProperty("gpxpath")
        private String gpxPath; // gpx 경로 (Course.gpxPath)
    }
}
