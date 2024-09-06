package com.amedvedev.mediaspace.media;

import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PostMediaMapper {

    @Mapping(source = "media.id", target = "id")
    @Mapping(source = "media.url", target = "url")
    @Mapping(source = "id.position", target = "position")
    ViewPostMediaResponse toViewPostMediaDto(PostMedia postMedia);
}
