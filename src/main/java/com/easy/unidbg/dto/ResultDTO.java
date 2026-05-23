package com.easy.unidbg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Unified API response wrapper.
 * All API endpoints return this structure. Null fields are omitted from JSON.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultDTO<T> {

    /** Response timestamp (epoch millis) */
    private Long ts;

    /** Error code (present only on failure) */
    private Integer errorCode;

    /** Error message (present only on failure) */
    private String errorMsg;

    /** "ok" or "fail" */
    private String status;

    /** Response payload (present only on success) */
    private T data;

    private ResultDTO() {
        setTs(System.currentTimeMillis());
        setStatus("ok");
    }

    private ResultDTO(T object) {
        setTs(System.currentTimeMillis());
        setStatus("ok");
        setData(object);
    }

    private ResultDTO(Integer errorCode) {
        setTs(System.currentTimeMillis());
        setErrorCode(errorCode);
        setStatus("fail");
    }

    private ResultDTO(Integer errorCode, String errorMsg) {
        setTs(System.currentTimeMillis());
        setErrorCode(errorCode);
        setErrorMsg(errorMsg);
        setStatus("fail");
    }

    public static <T> ResultDTO<T> success() {
        return new ResultDTO<>((T) "success");
    }

    public static <T> ResultDTO<T> success(T data) {
        return new ResultDTO<>(data);
    }

    public static <T> ResultDTO<T> error(Integer errorCode) {
        return new ResultDTO<>(errorCode);
    }

    public static <T> ResultDTO<T> error(Integer errorCode, String errorMsg) {
        return new ResultDTO<>(errorCode, errorMsg);
    }
}
