package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.story.Story;
import com.amedvedev.mediaspace.user.dto.ViewUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.WARN)
public interface UserMapper {

    @Mapping(source = "profilePicture.url", target = "profilePictureUrl")
    @Mapping(source = "posts", target = "postsIds", qualifiedByName = "postsToIds")
    @Mapping(source = "stories", target = "storiesIds", qualifiedByName = "storiesToIds")
    @Mapping(source = "followers", target = "followersCount", qualifiedByName = "followersCount")
    @Mapping(source = "following", target = "followingCount", qualifiedByName = "followingCount")
    ViewUserDto toViewUserDto(User user);



    @Named("postsToIds")
    default List<Long> postsToIds(List<Post> posts){
        return posts.stream()
                .map(Post::getId)
                .toList();
    }

    @Named("storiesToIds")
    default List<Long> storiesToIds(List<Story> stories){
        return stories.stream()
                .map(Story::getId)
                .toList();
    }

    @Named("followersCount")
    default long followersCount(List<User> followers){
        return followers.size();
    }

    @Named("followingCount")
    default long followingCount(List<User> following){
        return following.size();
    }
}
