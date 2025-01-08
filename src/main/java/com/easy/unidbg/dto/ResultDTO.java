package com.easy.unidbg.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultDTO<T> {

    private Long ts;

    private Integer errorCode;


    private String errorMsg;

    private String status;

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
