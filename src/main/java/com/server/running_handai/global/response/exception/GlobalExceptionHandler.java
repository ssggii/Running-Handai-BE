package com.server.running_handai.global.response.exception;

import com.server.running_handai.global.response.CommonResponse;
import com.server.running_handai.global.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.reactive.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** CustomException: Error Code에 정의된 비즈니스 로직 예외 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<?>> handleCustomException(BusinessException e) {
        return getErrorResponse(e, e.getResponseCode());
    }

    /**
     * BAD_REQUEST (400)
     * IllegalArgumentException: 사용자가 값을 잘못 입력한 경우
     * MethodArgumentNotValidException: 전달된 값이 유효하지 않은 경우
     * HttpMessageNotReadableException: 잘못된 형식으로 요청할 경우
     * MissingServletRequestParameterException: 필수 요청 매개변수가 누락된 경우
     * MethodArgumentTypeMismatchException: 요청 매개변수의 타입 변환을 실패한 경우
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<?>> handleIllegalArgumentException(IllegalArgumentException e) {
        return getErrorResponse(e, ResponseCode.ILLEGAL_ARGUMENT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<?>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        return getErrorResponse(e, ResponseCode.METHOD_ARGUMENT_NOT_VALID);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<?>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        return getErrorResponse(e, ResponseCode.HTTP_MESSAGE_NOT_READABLE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<?>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e) {
        return getErrorResponse(e, ResponseCode.MISSING_SERVLET_REQUEST_PARAMETER);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        return getErrorResponse(e, ResponseCode.ARGUMENT_TYPE_MISMATCH);
    }

    /**
     * METHOD_NOT_ALLOWED (405)
     * HttpRequestMethodNotSupportedException: 잘못된 Http Method를 가지고 요청할 경우
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        return getErrorResponse(e, ResponseCode.HTTP_REQUEST_METHOD_NOT_SUPPORTED);
    }

    /**
     * NOT_FOUND (404)
     * NoResourceFoundException: 존재하지 않는 리소스를 요청할 경우
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<CommonResponse<?>> handleNoResourceFoundException(
            NoResourceFoundException e) {
        return getErrorResponse(e, ResponseCode.RESOURCE_NOT_FOUND);
    }

    /**
     * INTERNAL_SERVER_ERROR (500)
     * RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CommonResponse<?>> handleRuntimeException(RuntimeException e) {
        return getErrorResponse(e, ResponseCode.REQUEST_SERVER);
    }

    // 예상하지 못한 모든 예외를 처리
    // 추후 자주 발생하는 오류에 대해 추가 필요
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<?>> handleException(Exception e) {
        return getErrorResponse(e, ResponseCode.REQUEST_SERVER);
    }

    // 예외 응답 처리
    private ResponseEntity<CommonResponse<?>> getErrorResponse(Exception e, ResponseCode responseCode) {
        CommonResponse<?> errorResponse = CommonResponse.error(responseCode);

        log.error(
                "Exception: {} | StatusCode: {} | ErrorCode: {} | Message: {}",
                e.getClass().getSimpleName(),
                errorResponse.getStatusCode(),
                errorResponse.getResponseCode(),
                e.getMessage()
        );

        return new ResponseEntity<>(errorResponse, responseCode.getHttpStatus());
    }
}
