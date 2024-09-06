package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.media.PostMedia;
import com.amedvedev.mediaspace.media.PostMediaMapper;
import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;


// abstract class to allow usage of another mapper
@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class PostMapper {

    private PostMediaMapper postMediaMapper;

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "postMediaList", target = "postMediaList", qualifiedByName = "mapPostMediaList")
    @Mapping(target = "likes", expression = "java(post.getLikedByUsers().size())")
    @Mapping(target = "commentsCount", expression = "java(post.getComments().size())")
    public abstract ViewPostResponse toViewPostResponse(Post post);

    @Named("mapPostMediaList")
    List<ViewPostMediaResponse> mapPostMediaList(List<PostMedia> postMediaList) {
        return postMediaList.stream()
                .map(postMedia -> postMediaMapper.toViewPostMediaDto(postMedia))
                .collect(Collectors.toList());
    }

    @Autowired
    public void setPostMediaMapper(PostMediaMapper postMediaMapper) {
        this.postMediaMapper = postMediaMapper;
    }
}
