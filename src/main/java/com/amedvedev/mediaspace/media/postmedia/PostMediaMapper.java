package com.amedvedev.mediaspace.media.postmedia;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import com.amedvedev.mediaspace.post.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.IntStream;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PostMediaMapper {

    @Mapping(target = "id", source = "media.id")
    @Mapping(target = "url", source = "media.url")
    @Mapping(target = "position", source = "id.position")
    ViewPostMediaResponse toViewPostMediaResponse(PostMedia postMedia);

    default List<PostMedia> mapUrlsToPostMedia(List<CreateMediaRequest> createMediaRequests, Post post) {
        return IntStream.range(0, createMediaRequests.size())
                .mapToObj(index -> {
                    var url = createMediaRequests.get(index).getUrl();
                    var media = Media.builder().url(url).build();
                    var mediaPostId = new PostMediaId(media.getId(), index + 1);
                    return new PostMedia(mediaPostId, media, post);
                })
                .toList();
    }
}
