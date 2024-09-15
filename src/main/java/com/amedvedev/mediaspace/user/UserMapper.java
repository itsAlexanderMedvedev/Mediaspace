package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.post.PostMapper;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.story.Story;
import com.amedvedev.mediaspace.user.dto.ViewUserResponse;
import org.hibernate.annotations.Array;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


// abstract class to allow usage of other mappers
@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class UserMapper {

    private PostMapper postMapper;

    @Mapping(source = "profilePicture.url", target = "profilePictureUrl")
    @Mapping(source = "posts", target = "posts", qualifiedByName = "getPosts")
    @Mapping(source = "stories", target = "storiesIds", qualifiedByName = "storiesToIds")
    @Mapping(source = "followers", target = "followersCount", qualifiedByName = "followersCount")
    @Mapping(source = "following", target = "followingCount", qualifiedByName = "followingCount")
    public abstract ViewUserResponse toViewUserDto(User user);


    @Named("getPosts")
    protected List<UserProfilePostResponse> getPosts(List<Post> posts){
        return posts.stream()
                .map(postMapper::toUserProfilePostResponse)
                .toList();
    }

    @Named("storiesToIds")
    protected List<Long> storiesToIds(List<Story> stories){
        return stories.stream()
                .map(Story::getId)
                .toList();
    }

    @Named("followersCount")
    protected long followersCount(List<User> followers){
        return followers.size();
    }

    @Named("followingCount")
    protected long followingCount(List<User> following){
        return following.size();
    }

    @Autowired
    public void setPostMapper(PostMapper postMapper) {
        this.postMapper = postMapper;
    }
}
