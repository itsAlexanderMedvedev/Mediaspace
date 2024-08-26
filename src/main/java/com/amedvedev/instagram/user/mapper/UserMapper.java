package com.amedvedev.instagram.user.mapper;

import com.amedvedev.instagram.post.Post;
import com.amedvedev.instagram.story.Story;
import com.amedvedev.instagram.user.User;
import com.amedvedev.instagram.user.dto.ViewUserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "profilePicture.url", target = "profilePictureUrl")
    @Mapping(source = "posts", target = "postsIds", qualifiedByName = "postsToIds")
    @Mapping(source = "stories", target = "storiesIds", qualifiedByName = "storiesToIds")
    @Mapping(target = "followersCount", expression = "java(user.getFollowers().size())")
    ViewUserDto toViewUserDto(User user);

    default List<Long> postsToIds(List<Post> posts){
        return posts.stream()
                .map(Post::getId)
                .toList();
    }

    default List<Long> storiesToIds(List<Story> stories){
        return stories.stream()
                .map(Story::getId)
                .toList();
    }
}
