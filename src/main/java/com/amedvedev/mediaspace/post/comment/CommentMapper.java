package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.post.comment.dto.ViewCommentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(source="user.username", target = "author")
    @Mapping(target="nestedComments", qualifiedByName = "commentsToDto")
    @Mapping(source="createdAt", target = "writtenAt")
    ViewCommentResponse toViewCommentResponse(Comment comment);

    @Named("commentsToDto")
    default List<ViewCommentResponse> commentsToDto(List<Comment> comments) {
        return comments.stream().map(this::toViewCommentResponse).toList();
    }
}
