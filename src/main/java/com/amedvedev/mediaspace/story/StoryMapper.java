package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.dto.ViewStoriesFeedResponse;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface StoryMapper {

    @Mapping(source = "media.url", target = "mediaUrl")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "createdAt", target = "createdAt", dateFormat = "dd.MM.yyyy HH:mm")
    ViewStoryResponse toViewStoryResponse(Story story);

    @Mapping(source = "id", target = "storyId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.profilePicture", target = "userPictureUrl", qualifiedByName = "getProfilePicture")
    ViewStoriesFeedResponse toViewStoriesFeedResponse(Story stories);

    @Mapping(source = "media.url", target = "mediaUrl")
    StoryDto toStoryDto(Story story);

    @Named("getProfilePicture")
    default String getProfilePicture(Media profilePicture) {
        return profilePicture == null ? null : profilePicture.getUrl();
    }
}

