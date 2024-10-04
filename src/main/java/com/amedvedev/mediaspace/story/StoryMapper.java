package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.dto.ViewStoriesFeedResponse;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface StoryMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "media.url", target = "mediaUrl")
    ViewStoryResponse toViewStoryDto(Story story);

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.profilePicture", target = "userPictureUrl", qualifiedByName = "getProfilePicture")
    @Mapping(source = "id", target = "storyId")
    ViewStoriesFeedResponse toViewStoriesFeedResponse(Story stories);

    @Named("getProfilePicture")
    default String getProfilePicture(Media profilePicture) {
        return profilePicture == null ? null : profilePicture.getUrl();
    }
}

