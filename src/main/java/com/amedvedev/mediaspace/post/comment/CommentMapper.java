package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.post.comment.dto.ViewCommentResponse;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
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

    @Mapping(target = "comments", qualifiedByName = "mapComments")
    ViewPostCommentsResponse toViewPostCommentsResponse(List<Comment> comments, Long postId);

    @Named("commentsToDto")
    default List<ViewCommentResponse> commentsToDto(List<Comment> comments) {
        return comments.stream().map(this::toViewCommentResponse).toList();
    }

    @Named("mapComments")
    default List<ViewCommentResponse> mapComments(List<Comment> comments) {
        return comments.stream()
                .map(this::toViewCommentResponse)
//                .sorted(Comparator.comparing(CommentDto::getWrittenAt).reversed())
                .toList();
    }
}
