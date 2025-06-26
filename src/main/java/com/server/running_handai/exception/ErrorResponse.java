package com.server.running_handai.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
public class ErrorResponse {
    private LocalDateTime timeStamp;
    private int status;
    private String error;
    private String code;
    private String message;
}
