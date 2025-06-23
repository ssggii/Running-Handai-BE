package com.server.running_handai.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    // BAD_REQUEST (400): 잘못된 요청

    // UNAUTHORIZED (401): 인증되지 않은 사용자

    // FORBIDDEN (403): 허용되지 않은 접근

    // NOT_FOUND (404): 잘못된 리소스 접근
    REVIEW_NOT_FOUND(NOT_FOUND, "찾을 수 없는 리뷰입니다."),
    MEMBER_NOT_FOUND(NOT_FOUND, "찾을 수 없는 사용자입니다.");

    // CONFLICT (409): 중복된 리소스, 요청이 현재 서버 상태와 충돌될 때

    // INTERNAL SERVER ERROR (500)

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
