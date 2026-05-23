package com.easy.unidbg.dto;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/** Request body DTO for POST /api/common/invoke endpoints. */
@Data
@Validated
public class InvokeVo {
    /** Fully qualified class name of the module to invoke */
    @NotBlank
    private String module;
    /** Arguments passed to the module's main() method */
    @NotNull
    private List<String> args;
}
