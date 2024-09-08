package com.amedvedev.mediaspace.post.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private Long id;
    private String body;
    private String author;
    private String writtenAt;
    private List<CommentDto> comments;
}
