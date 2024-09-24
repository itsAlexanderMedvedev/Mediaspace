package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.post.PostMapper;
import com.amedvedev.mediaspace.post.PostService;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.story.Story;
import com.amedvedev.mediaspace.story.StoryService;
import com.amedvedev.mediaspace.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class UserMapper {

//    private PostMapper postMapper;
//    private UserRepository userRepository;
//    private PostService postService;
//    private StoryService storyService;

    @Mapping(source = "profilePicture.url", target = "profilePictureUrl")
//    @Mapping(source = "id", target = "posts", qualifiedByName = "getPosts")
    @Mapping(source = "id", target = "storiesIds", qualifiedByName = "storiesToIds")
//    @Mapping(source = "id", target = "followersCount", qualifiedByName = "followersCount")
//    @Mapping(source = "id", target = "followingCount", qualifiedByName = "followingCount")
    public abstract UserDto toUserDto(User user,
                                      List<UserProfilePostResponse> posts,
                                      List<Long> storiesIds,
                                      long followersCount,
                                      long followingCount);

    @Mapping(source = "profilePictureUrl", target = "profilePicture", qualifiedByName = "getProfilePicture")
    public abstract User toUser(UserDto userDto);


//    @Named("getPosts")
//    protected List<UserProfilePostResponse> getPosts(Long id) {
//        var posts = postService.getPostsByUserId(id);
//        return posts.stream()
//                .map(postMapper::toUserProfilePostResponse)
//                .toList();
//    }

//    @Named("storiesToIds")
//    protected List<Long> storiesToIds(Long id) {
//        return storyService.getStoriesByUserId(id).stream()
//                .map(Story::getId)
//                .toList();
//    }

//    @Named("followersCount")
//    protected long followersCount(Long id) {
//        return userRepository.countFollowersByUserId(id);
//    }

//    @Named("followingCount")
//    protected long followingCount(Long id) {
//        return userRepository.countFollowingByUserId(id);
//    }

    @Named("getProfilePicture")
    protected Media getProfilePicture(String profilePictureUrl) {
        return profilePictureUrl == null ? null : Media.builder().url(profilePictureUrl).build();
    }

//    @Autowired
//    public void setPostMapper(PostMapper postMapper) {
//        this.postMapper = postMapper;
//    }
//
//    @Autowired
//    public void setUserRepository(UserRepository userRepository) {
//        this.userRepository = userRepository;
//    }
//
//    @Autowired
//    public void setPostService(PostService postService) {
//        this.postService = postService;
//    }
//
//    @Autowired
//    public void setStoryService(StoryService storyService) {
//        this.storyService = storyService;
//    }
}
