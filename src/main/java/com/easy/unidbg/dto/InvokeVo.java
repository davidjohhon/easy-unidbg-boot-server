package com.easy.unidbg.dto;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Validated
public class InvokeVo {
    @NotBlank
    private String module;
    @NotNull
    private List<String> args;
}
