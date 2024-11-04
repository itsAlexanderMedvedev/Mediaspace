package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.story.projection.StoryFeedProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByUserId(Long id);

//    @Query("""
//            SELECT s FROM Story s
//            WHERE s.user.id IN (
//                SELECT f.followee.id
//                FROM Follow f
//                WHERE f.follower.id = :userId)
//            ORDER BY s.createdAt DESC
//            """)
//    List<Story> findStoriesFeed(@Param("userId") Long userId);

    @Query("""
        SELECT s.user.username AS username,
               COALESCE(p.url, '') AS profilePictureUrl
        FROM Story s
        LEFT JOIN s.user.profilePicture p
        WHERE s.user.id IN (
            SELECT f.followee.id
            FROM Follow f
            WHERE f.follower.id = :userId)
        """)
    List<StoryFeedProjection> findStoryFeedByUserId(Long userId);

    @Query("SELECT s.id FROM Story s WHERE s.user.id = :userId")
    List<Long> findStoriesIdsByUserId(Long userId);
}
