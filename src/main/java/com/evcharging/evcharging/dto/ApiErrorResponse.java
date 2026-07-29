package com.evcharging.evcharging.dto;

import java.util.Map;

public class ApiErrorResponse {

    public String error;
    public String message;
    public Map<String, String> fields;
    public Integer attemptsRemaining;
    public Long retryAfterSeconds;

    public ApiErrorResponse(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public ApiErrorResponse withFields(Map<String, String> fields) {
        this.fields = fields;
        return this;
    }

    public ApiErrorResponse withAttemptsRemaining(int attemptsRemaining) {
        this.attemptsRemaining = attemptsRemaining;
        return this;
    }

    public ApiErrorResponse withRetryAfterSeconds(long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
        return this;
    }
}
