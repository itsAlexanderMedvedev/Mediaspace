package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.user.dto.UserDto;
import com.amedvedev.mediaspace.user.dto.ViewUserProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(source = "profilePicture.url", target = "profilePictureUrl")
    UserDto toUserDto(User user);
    
    ViewUserProfileResponse toViewUserProfileResponse(UserDto user,
                                                      List<UserProfilePostResponse> posts,
                                                      List<Long> storiesIds,
                                                      long followersCount,
                                                      long followingCount);

    @Named("getProfilePicture")
    default Media getProfilePicture(String profilePictureUrl) {
        return profilePictureUrl == null ? null : Media.builder().url(profilePictureUrl).build();
    }
}
