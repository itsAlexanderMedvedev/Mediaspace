package com.amedvedev.mediaspace.user.follow;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowRepository extends CrudRepository<Follow, FollowId> {

    @Query("SELECT f.follower.id FROM Follow f WHERE f.followee.id = :userId")
    List<Long> findFollowersIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followee.id = :userId")
    int countFollowersByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower.id = :userId")
    int countFollowingByUserId(@Param("userId") Long userId);
}
