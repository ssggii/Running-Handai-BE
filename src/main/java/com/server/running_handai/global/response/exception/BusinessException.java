package com.server.running_handai.global.response.exception;

import com.server.running_handai.global.response.ResponseCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{
    private final ResponseCode responseCode;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }
}
