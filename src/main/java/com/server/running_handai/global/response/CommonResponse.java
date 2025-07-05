package com.server.running_handai.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Collection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    private final int statusCode;
    private final String responseCode;
    private final String message;
    private final Integer totalCount;
    private final T data;

    // 성공 응답 생성자
    private CommonResponse(ResponseCode responseCode, T data) {
        this.statusCode = responseCode.getHttpStatus().value();
        this.responseCode = responseCode.getCode();
        this.message = responseCode.getMessage();
        this.data = data;

        if (data instanceof Collection) {
            this.totalCount = ((Collection<?>) data).size();
        } else {
            this.totalCount = null; // data가 Collection이 아닐 경우 count는 null (JSON 응답에 포함되지 않음)
        }
    }

    // 실패 응답 생성자
    private CommonResponse(ResponseCode responseCode) {
        this.statusCode = responseCode.getHttpStatus().value();
        this.responseCode = responseCode.getCode();
        this.message = responseCode.getMessage();
        this.data = null;
        this.totalCount = null;
    }

    // 성공 응답을 위한 정적 팩토리 메서드
    public static <T> CommonResponse<T> success(ResponseCode responseCode, T data) {
        return new CommonResponse<>(responseCode, data);
    }

    // 실패 응답을 위한 정적 팩토리 메서드
    public static <T> CommonResponse<T> error(ResponseCode responseCode) {
        return new CommonResponse<>(responseCode);
    }
}
