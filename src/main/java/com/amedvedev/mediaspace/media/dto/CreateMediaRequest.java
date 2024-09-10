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
public class CreateMediaRequest {

    @URL(message = "Invalid URL")
    @Schema(description = "The URL of the media content", example = "https://www.your-resource.com/image.jpg")
    private String url;
}
