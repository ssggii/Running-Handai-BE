package com.server.running_handai.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@AllArgsConstructor
@Getter
public enum ResponseCode {
    /** 성공 시 응답 코드 */
    SUCCESS(OK, "요청을 성공했습니다."),
    SUCCESS_EMPTY_COURSE_INFO(OK, "코스 조회 결과가 없습니다."),
    SUCCESS_COURSE_SYNC_ACCEPTED(ACCEPTED, "코스 데이터 동기화 작업이 시작되었습니다. 서버 로그를 통해 진행 상황을 확인하세요."),

    /** 비즈니스 에러 코드 */
    // BAD_REQUEST (400)
    INVALID_USER_POINT(BAD_REQUEST, "사용자의 위치 좌표가 올바르지 않습니다."),
    INVALID_AREA_PARAMETER(BAD_REQUEST, "지역 파라미터가 올바르지 않습니다."),
    INVALID_THEME_PARAMETER(BAD_REQUEST, "테마 파라미터가 올바르지 않습니다."),
    INVALID_COURSE_FILTER_TYPE(BAD_REQUEST, "코스 필터링 옵션이 올바르지 않습니다."),

    // NOT_FOUND (404)
    AREA_NOT_FOUND(NOT_FOUND, "지원하지 않는 지역입니다."),
    COURSE_NOT_FOUND(NOT_FOUND, "찾을 수 없는 코스입니다."),

    /** 시스템 및 공통 예외용 에러 코드 */
    // BAD_REQUEST (400)
    ILLEGAL_ARGUMENT(BAD_REQUEST, "잘못된 인자 값입니다."),
    METHOD_ARGUMENT_NOT_VALID(BAD_REQUEST, "유효하지 않은 인자 값입니다."),
    HTTP_MESSAGE_NOT_READABLE(BAD_REQUEST, "잘못된 요청 형식입니다."),
    MISSING_SERVLET_REQUEST_PARAMETER(BAD_REQUEST, "필수 요청 매개변수가 누락되었습니다."),
    ARGUMENT_TYPE_MISMATCH(BAD_REQUEST, "요청 매개변수의 타입이 올바르지 않습니다."),

    // METHOD_NOT_ALLOWED (405)
    HTTP_REQUEST_METHOD_NOT_SUPPORTED(METHOD_NOT_ALLOWED, "잘못된 HTTP Method 요청입니다."),

    // INTERNAL SERVER ERROR (500)
    REQUEST_SERVER(INTERNAL_SERVER_ERROR, "서버에 요청 부탁드립니다."),
    OPENAI_API_ERROR(INTERNAL_SERVER_ERROR, "OpenAI API 호출에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
