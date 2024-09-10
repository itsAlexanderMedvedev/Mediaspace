package com.amedvedev.mediaspace.exception.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneralErrorResponse {

    @Schema(description = "The error message", example = "The request could not be processed")
    private String reason;

    @Schema(description = "The timestamp of the error")
    private LocalDateTime timestamp;
}
