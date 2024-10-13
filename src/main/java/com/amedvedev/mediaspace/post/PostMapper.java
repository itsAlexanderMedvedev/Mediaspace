package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PostMapper {

    @Mapping(target = "postMediaList", source = "postMediaList")
    @Mapping(target = "username", source = "post.user.username")
    @Mapping(target = "likes", expression = "java(post.getLikes().size())")
    @Mapping(target = "commentsCount", expression = "java(post.getComments().size())")
    @Mapping(target = "createdAt", source = "post.createdAt", dateFormat = "dd.MM.yyyy HH:mm")
    ViewPostResponse toViewPostResponse(Post post, List<ViewPostMediaResponse> postMediaList);

    @Mapping(target = "coverImage", expression = "java(post.getPostMediaList().get(0).getMedia().getUrl())")
    UserProfilePostResponse toUserProfilePostResponse(Post post);
}