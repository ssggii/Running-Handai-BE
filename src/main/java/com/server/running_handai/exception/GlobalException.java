package com.server.running_handai.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
