package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.post.comment.dto.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(source="user.username", target = "author")
    @Mapping(target="comments", qualifiedByName = "commentsToDto")
    @Mapping(source="createdAt", target = "writtenAt")
    CommentDto toCommentDto(Comment comment);

    @Named("commentsToDto")
    default List<CommentDto> commentsToDto(List<Comment> comments) {
        return comments.stream().map(this::toCommentDto).toList();
    }
}
