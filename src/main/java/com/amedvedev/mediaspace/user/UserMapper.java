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

    @Mapping(target = "profilePictureUrl", expression = "java(user.getProfilePictureUrl())")
    UserDto toUserDto(User user);
    
    ViewUserProfileResponse toViewUserProfileResponse(UserDto user,
                                                      List<UserProfilePostResponse> posts,
                                                      List<Long> storiesIds,
                                                      int followersCount,
                                                      int followingCount);
}
