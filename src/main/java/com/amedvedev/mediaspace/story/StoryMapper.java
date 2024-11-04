package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.story.dto.StoriesFeedEntry;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.dto.StoryPreviewResponse;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import com.amedvedev.mediaspace.story.projection.StoryFeedProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    
    StoriesFeedEntry toStoryFeedResponse(StoryFeedProjection storyFeedProjection);

    default Set<ZSetOperations.TypedTuple<Object>> mapStoriesFeedProjectionsToTuples(
            Set<StoriesFeedEntry> storyFeedEntries) {
        
        return storyFeedEntries.stream()
                .map(storiesFeedEntry ->
                        new DefaultTypedTuple<>((Object) storiesFeedEntry, (double) Instant.now().toEpochMilli()))
                .collect(Collectors.toSet());
    }

    default List<Long> mapStoriesIdsObjectsToLong(Collection<Object> stories) {
        return stories.stream()
                .map(String::valueOf)
                .map(Long::parseLong)
                .toList();
    }
    
    default List<StoriesFeedEntry> mapTuplesToStoriesFeed(Collection<Object> tuples) {
        return tuples.stream()
                .map(StoriesFeedEntry.class::cast)
                .collect(Collectors.toList());
    }
}

