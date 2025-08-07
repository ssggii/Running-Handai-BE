package com.server.running_handai.domain.spot.dto;

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
public class SpotLocationApiResponseDto {

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
     * [국문 관광정보] 위치기반 관광정보 조회
     */
    @Getter
    @ToString
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        @JsonProperty("contentid")
        private String spotExternalId; // 장소 고유번호 (Spot.externalId)
    }
}