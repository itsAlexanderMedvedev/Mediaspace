package com.amedvedev.mediaspace.story;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByUserId(Long id);

    @Query("SELECT s FROM Story s WHERE s.user.id IN (SELECT f.followee.id FROM Follow f WHERE f.follower.id = :userId)")
    List<Story> findStoriesFeed(@Param("userId") Long userId);
}
