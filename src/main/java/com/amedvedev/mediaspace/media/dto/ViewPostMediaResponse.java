package com.amedvedev.mediaspace.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewPostMediaResponse {

    @Schema(description = "The media ID", example = "1")
    private Long id;

    @Schema(description = "The media URL", example = "https://www.example.com/media.jpg")
    private String url;

    @Schema(description = "The media position in the post", example = "1")
    private Integer position;
}
