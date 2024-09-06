package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface StoryMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "media.url", target = "mediaUrl")
    ViewStoryResponse toViewStoryDto(Story story);
}

