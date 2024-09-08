package com.amedvedev.mediaspace.media.postmedia;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PostMediaRepository extends CrudRepository<PostMedia, PostMediaId> {
    List<PostMedia> findAllByPostId(Long postId);
}
