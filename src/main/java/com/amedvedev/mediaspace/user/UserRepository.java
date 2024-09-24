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

    @Query(value = "SELECT * FROM _user", nativeQuery = true)
    List<User> findAllIncludingSoftDeleted();

    // Query to avoid soft deleted users being excluded in order to tell the user that the account can be restored
    @Query(value = "SELECT * FROM _user WHERE LOWER(username) = LOWER(:username)", nativeQuery = true)
    Optional<User> findByUsernameIgnoreCaseAndIncludeSoftDeleted(@Param("username") String username);

    @Query("SELECT u.followers FROM User u WHERE u.id = :userId")
    List<User> findFollowersByUserId(@Param("userId") Long userId);

    @Query("SELECT u.following FROM User u WHERE u.id = :userId")
    List<User> findFollowingByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.id = :userId")
    long countFollowersByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM User u JOIN u.following f WHERE u.id = :userId")
    long countFollowingByUserId(@Param("userId") Long userId);

    @Query("SELECT f.id FROM User u JOIN u.following f WHERE u.id = :userId")
    List<Long> findFollowingIdsByUserId(@Param("userId") Long userId);
}
