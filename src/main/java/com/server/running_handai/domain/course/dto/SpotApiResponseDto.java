package com.server.running_handai.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Hidden
@Getter
@ToString
@NoArgsConstructor
public class SpotApiResponseDto {

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
     * [국문 관광정보] 공통정보 조회
     */
    @Getter
    @ToString
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        @JsonProperty("contentid")
        private String spotExternalId; // 장소 고유번호 (Spot.externalId)

        @JsonProperty("title")
        private String spotName; // 장소 이름 (Spot.name)

        @JsonProperty("overview")
        private String spotDescription; // 장소 설명 (Spot.description)

        @JsonProperty("addr1")
        private String spotAddress; // 장소 주소 (Spot.address)

        @JsonProperty("cat3")
        private String spotCategoryNumber; // 장소 카테고리 (Spot.category)

        @JsonProperty("firstimage")
        private String spotOriginalImage; // 대표 이미지 - 원본 (SpotImage.imageUrl)

        @JsonProperty("firstimage2")
        private String spotThumbnailImage; // 대표 이미지 - 썸네일 (SpotImage.imageUrl)

        @JsonProperty("mapx")
        private String spotLongitude; // 장소 경도 좌표 (Spot.lon)

        @JsonProperty("mapy")
        private String spotLatitude; // 장소 위도 좌표 (Spot.lat)
    }
}