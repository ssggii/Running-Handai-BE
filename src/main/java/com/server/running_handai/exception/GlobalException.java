package com.server.running_handai.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalException {
    /** CustomException: Error Code에 정의된 비즈니스 로직 예외 */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        return getErrorResponse(e, errorCode.getHttpStatus(), errorCode.getCode());
    }

    /**
     * BAD_REQUEST (400)
     * IllegalArgumentException: 사용자가 값을 잘못 입력한 경우
     * MethodArgumentNotValidException: 전달된 값이 유효하지 않은 경우
     * HttpMessageNotReadableException: 잘못된 형식으로 요청할 경우
     * MissingServletRequestParameterException: 필수 요청 매개변수가 누락된 경우
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return getErrorResponse(e, HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        return getErrorResponse(e, HttpStatus.BAD_REQUEST, "METHOD_ARGUMENT_NOT_VALID");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        return getErrorResponse(e, HttpStatus.BAD_REQUEST, "HTTP_MESSAGE_NOT_READABLE");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        return getErrorResponse(e, HttpStatus.BAD_REQUEST, "MISSING_SERVLET_REQUEST_PARAMETER");
    }

    /**
     * METHOD_NOT_ALLOWED (405)
     * HttpRequestMethodNotSupportedException: 잘못된 Http Method를 가지고 요청할 경우
     */
    // spotless:on
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        return getErrorResponse(e, HttpStatus.METHOD_NOT_ALLOWED, "HTTP_REQUEST_METHOD_NOT_SUPPORTED");
    }

    /**
     * INTERNAL_SERVER_ERROR (500)
     * RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        return getErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR, "REQUEST_SERVER");
    }

    // 예상하지 못한 모든 예외를 처리
    // 추후 자주 발생하는 오류에 대해 추가 필요
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return getErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR, "REQUEST_SERVER");
    }

    // 일관된 예외 응답 처리
    private ResponseEntity<ErrorResponse> getErrorResponse(Exception e, HttpStatus httpStatus, String code) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                httpStatus.value(),
                httpStatus.name(),
                code,
                e.getMessage()
        );

        log.error(
                "Exception: {} timeStamp: {} status: {} error: {} code: {} message: {}",
                e.getClass().getSimpleName(),
                LocalDateTime.now(),
                httpStatus.value(),
                httpStatus.name(),
                code,
                e.getMessage()
        );
        return new ResponseEntity<>(errorResponse, httpStatus);
    }
}
