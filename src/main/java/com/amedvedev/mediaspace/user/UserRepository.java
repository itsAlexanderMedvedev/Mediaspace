package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.post.Post;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);

    // Query to avoid soft deleted users being excluded in order to tell the user that the account can be restored
    @Query(value = "SELECT * FROM _user WHERE LOWER(username) = LOWER(:username)", nativeQuery = true)
    Optional<User> findByUsernameIgnoreCaseAndIncludeSoftDeleted(@Param("username") String username);

    @Query("SELECT f.follower.id FROM Follow f WHERE f.followee.id = :userId")
    List<Long> findFollowersIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.followee = :userId")
    int countFollowersByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.follower = :userId")
    int countFollowingByUserId(@Param("userId") Long userId);
}
