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
    SUCCESS_BOOKMARK_CREATE(OK, "북마크 등록 완료했습니다."),
    SUCCESS_BOOKMARK_DELETE(OK, "북마크 해제 완료했습니다."),
    SUCCESS_EMPTY_REVIEWS(OK, "리뷰 조회 결과가 없습니다."),
    SUCCESS_EMPTY_BOOKMARKS(OK, "북마크한 코스가 없습니다."),
    SUCCESS_COURSE_REMOVE(OK, "코스 삭제가 완료되었습니다."),

    /** 비즈니스 에러 코드 */
    // BAD_REQUEST (400)
    INVALID_USER_POINT(BAD_REQUEST, "사용자의 위치 좌표가 올바르지 않습니다."),
    INVALID_AREA_PARAMETER(BAD_REQUEST, "지역 파라미터가 올바르지 않습니다."),
    INVALID_THEME_PARAMETER(BAD_REQUEST, "테마 파라미터가 올바르지 않습니다."),
    INVALID_COURSE_FILTER_TYPE(BAD_REQUEST, "코스 필터링 옵션이 올바르지 않습니다."),
    ALREADY_BOOKMARKED(BAD_REQUEST, "이미 북마크한 코스입니다."),
    INVALID_PROVIDER(BAD_REQUEST, "지원하지 않는 OAuth2 Provider입니다"),
    INVALID_REVIEW_STARS(BAD_REQUEST, "별점은 0.5점 단위여야합니다."),
    EMPTY_REVIEW_CONTENTS(BAD_REQUEST, "리뷰 내용은 비워둘 수 없습니다"),
    BAD_REQUEST_STATE_PARAMETER(BAD_REQUEST, "로그인 요청 시 유효한 state 값이 필요합니다."),
    INVALID_NICKNAME_LENGTH(BAD_REQUEST, "닉네임은 2글자부터 10글자까지 입력할 수 있습니다."),
    INVALID_NICKNAME_FORMAT(BAD_REQUEST, "닉네임은 영문, 한글, 숫자만 입력할 수 있습니다."),
    SAME_AS_CURRENT_NICKNAME(BAD_REQUEST, "현재 사용 중인 닉네임과 동일합니다."),
    EMPTY_FILE(BAD_REQUEST, "파일이 누락되었습니다."),
    INVALID_POINT_NAME(BAD_REQUEST, "포인트 이름이 누락되었습니다."),
    DUPLICATE_COURSE_NAME(BAD_REQUEST, "이미 존재하는 코스 이름입니다."),

    // UNAUTHORIZED (401)
    INVALID_ACCESS_TOKEN(UNAUTHORIZED, "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(UNAUTHORIZED, "유효하지 않은 리프래시 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(UNAUTHORIZED, "만료된 리프래시 토큰입니다."),
    UNAUTHORIZED_ACCESS(UNAUTHORIZED, "인증이 필요합니다."),

    // FORBIDDEN (403)
    ACCESS_DENIED(FORBIDDEN, "접근 권한이 없습니다."),
    NOT_COURSE_CREATOR(FORBIDDEN, "해당 코스를 만든 사용자가 아닙니다."),
    NO_AUTHORITY_TO_DELETE_COURSE(FORBIDDEN, "코스 삭제 권한이 없습니다."),

    // NOT_FOUND (404)
    COURSE_NOT_FOUND(NOT_FOUND, "찾을 수 없는 코스입니다."),
    MEMBER_NOT_FOUND(NOT_FOUND, "찾을 수 없는 사용자입니다."),
    REFRESH_TOKEN_NOT_FOUND(NOT_FOUND, "찾을 수 없는 리프래시 토큰입니다."),
    BOOKMARK_NOT_FOUND(NOT_FOUND, "찾을 수 없는 북마크입니다."),
    REVIEW_NOT_FOUND(NOT_FOUND, "찾을 수 없는 리뷰입니다."),

    // CONFLICT (409)
    DUPLICATE_NICKNAME(CONFLICT, "이미 사용 중인 닉네임입니다."),

    /** 시스템 및 공통 예외용 에러 코드 */
    // BAD_REQUEST (400)
    ILLEGAL_ARGUMENT(BAD_REQUEST, "잘못된 인자 값입니다."),
    HTTP_MESSAGE_NOT_READABLE(BAD_REQUEST, "잘못된 요청 형식입니다."),
    MISSING_SERVLET_REQUEST_PARAMETER(BAD_REQUEST, "필수 요청 매개변수가 누락되었습니다."),
    ARGUMENT_TYPE_MISMATCH(BAD_REQUEST, "요청 매개변수의 타입이 올바르지 않습니다."),
    OPENAI_RESPONSE_INVALID(BAD_REQUEST, "OPEN AI 응답값이 유효하지 않습니다."),
    INVALID_INPUT_VALUE(BAD_REQUEST, "유효하지 않은 입력 값입니다."),

    // NOT_FOUND (404)
    RESOURCE_NOT_FOUND(NOT_FOUND, "존재하지 않는 리소스입니다."),
    TRACK_POINTS_NOT_FOUND(NOT_FOUND, "파싱된 트랙 포인트가 없습니다."),

    // METHOD_NOT_ALLOWED (405)
    HTTP_REQUEST_METHOD_NOT_SUPPORTED(METHOD_NOT_ALLOWED, "잘못된 HTTP Method 요청입니다."),

    // INTERNAL SERVER ERROR (500)
    REQUEST_SERVER(INTERNAL_SERVER_ERROR, "서버에 요청 부탁드립니다."),
    OPENAI_API_ERROR(INTERNAL_SERVER_ERROR, "OpenAI API 호출에 실패했습니다."),
    FILE_UPLOAD_FAILED(INTERNAL_SERVER_ERROR, "파일 업로드를 실패했습니다."),
    FILE_DELETE_FAILED(INTERNAL_SERVER_ERROR, "파일 삭제를 실패했습니다."),
    GPX_FILE_PARSE_FAILED(INTERNAL_SERVER_ERROR, "GPX 파일 파싱을 실패했습니다"),
    PRESIGEND_URL_FAILED(INTERNAL_SERVER_ERROR, "Presigned Url 발급을 실패했습니다."),
    UNSUPPORTED_FILE_TYPE(INTERNAL_SERVER_ERROR, "지원하지 않는 파일 Content Type입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return this.name();
    }
}
