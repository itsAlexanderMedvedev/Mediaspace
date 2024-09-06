package com.amedvedev.mediaspace.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewPostMediaResponse {
    
    private Long id;
    private String url;
    private Integer position;
}
