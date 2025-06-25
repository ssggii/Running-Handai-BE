package com.server.running_handai.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    /** 비즈니스 에러 코드 */
    // NOT_FOUND (404)
    REVIEW_NOT_FOUND(NOT_FOUND, "찾을 수 없는 리뷰입니다."),

    /** 시스템 및 공통 예외용 에러 코드 */
    // BAD_REQUEST (400)
    ILLEGAL_ARGUMENT(BAD_REQUEST, "잘못된 인자 값입니다."),
    METHOD_ARGUMENT_NOT_VALID(BAD_REQUEST, "유효하지 않은 인자 값입니다."),
    HTTP_MESSAGE_NOT_READABLE(BAD_REQUEST, "잘못된 요청 형식입니다."),
    MISSING_SERVLET_REQUEST_PARAMETER(BAD_REQUEST, "필수 요청 매개변수가 누락되었습니다."),

    // METHOD_NOT_ALLOWED (405)
    HTTP_REQUEST_METHOD_NOT_SUPPORTED(METHOD_NOT_ALLOWED, "잘못된 HTTP Method 요청입니다."),

    // INTERNAL SERVER ERROR (500)
    REQUEST_SERVER(INTERNAL_SERVER_ERROR, "서버에 요청 부탁드립니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
