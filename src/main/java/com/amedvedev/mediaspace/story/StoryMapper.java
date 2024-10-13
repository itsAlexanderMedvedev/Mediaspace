package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.dto.StoryPreviewResponse;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Collection;
import java.util.List;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface StoryMapper {

    @Mapping(target = "mediaUrl", source = "media.url")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "createdAt", source = "createdAt", dateFormat = "dd.MM.yyyy HH:mm")
    ViewStoryResponse toViewStoryResponse(Story story);

    ViewStoryResponse toViewStoryResponse(StoryDto story);

    @Mapping(target = "storyId", source = "id")
    @Mapping(target = "username", source = "user.username")
    StoryPreviewResponse toStoryPreviewResponse(Story story);

    @Mapping(target = "storyId", source = "id")
    StoryPreviewResponse toStoryPreviewResponse(StoryDto story);

    @Mapping(target = "mediaUrl", source = "media.url")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "createdAt", source = "createdAt", dateFormat = "dd.MM.yyyy HH:mm")
    StoryDto toStoryDto(Story story);

    @Named("getProfilePicture")
    default String getProfilePicture(Media profilePicture) {
        return profilePicture == null ? null : profilePicture.getUrl();
    }

    default List<Long> mapStoriesIdsObjectsToLong(Collection<Object> stories) {
        return stories.stream()
                .map(String::valueOf)
                .map(Long::parseLong)
                .toList();
    }
}

