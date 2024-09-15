package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.media.postmedia.PostMedia;
import com.amedvedev.mediaspace.media.postmedia.PostMediaMapper;
import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import com.amedvedev.mediaspace.post.comment.Comment;
import com.amedvedev.mediaspace.post.comment.CommentMapper;
import com.amedvedev.mediaspace.post.comment.dto.CommentDto;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

// abstract class to allow usage of other mappers
@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class PostMapper {

    private PostMediaMapper postMediaMapper;
    private CommentMapper commentMapper;

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "postMediaList", target = "postMediaList", qualifiedByName = "mapPostMediaList")
    @Mapping(target = "likes", expression = "java(post.getLikes().size())")
    @Mapping(target = "commentsCount", expression = "java(post.getComments().size())")
    public abstract ViewPostResponse toViewPostResponse(Post post);

    @Mapping(source = "id", target = "postId")
    @Mapping(target = "comments", qualifiedByName = "mapComments")
    public abstract ViewPostCommentsResponse toViewPostCommentsResponse(Post post);

    @Mapping(target = "coverImage", expression = "java(post.getPostMediaList().get(0).getMedia().getUrl())")
    public abstract UserProfilePostResponse toUserProfilePostResponse(Post post);

    @Named("mapPostMediaList")
    protected List<ViewPostMediaResponse> mapPostMediaList(List<PostMedia> postMediaList) {
        return postMediaList.stream()
                .map(postMedia -> postMediaMapper.toViewPostMediaDto(postMedia))
                .toList();
    }

    @Named("mapComments")
    protected List<CommentDto> mapComments(List<Comment> comments) {
        return comments.stream()
                .map(commentMapper::toCommentDto)
//                .sorted(Comparator.comparing(CommentDto::getWrittenAt).reversed())
                .toList();
    }

    @Autowired
    public void setPostMediaMapper(PostMediaMapper postMediaMapper) {
        this.postMediaMapper = postMediaMapper;
    }

    @Autowired
    public void setCommentMapper(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }
}
