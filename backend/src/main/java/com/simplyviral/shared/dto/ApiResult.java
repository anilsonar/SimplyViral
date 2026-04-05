package com.simplyviral.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {
    private boolean success;
    private T data;
    private String error;
    private String errorCode;

    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResult<T> error(String message, String errorCode) {
        return ApiResult.<T>builder().success(false).error(message).errorCode(errorCode).build();
    }
}
