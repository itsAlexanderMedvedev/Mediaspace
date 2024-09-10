package com.amedvedev.mediaspace.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationErrorResponse {

    @Schema(description = "The error message")
    private Map<String, String> errors;

    @Schema(description = "The timestamp of the error")
    private LocalDateTime timestamp;
}
