package com.swiftvault.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse() { this.timestamp = LocalDateTime.now(); }

    public ApiResponse(boolean success, String message, T data) {
        this.success   = success;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess()          { return success; }
    public String getMessage()          { return message; }
    public T getData()                  { return data; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setSuccess(boolean v)          { this.success = v; }
    public void setMessage(String v)           { this.message = v; }
    public void setData(T v)                   { this.data = v; }
    public void setTimestamp(LocalDateTime v)  { this.timestamp = v; }
}
