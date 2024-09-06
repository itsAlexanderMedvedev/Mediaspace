package com.amedvedev.mediaspace.story;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByUserId(Long id);
}
