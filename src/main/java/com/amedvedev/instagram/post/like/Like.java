package com.amedvedev.instagram.post.like;

import com.amedvedev.instagram.post.Post;
import com.amedvedev.instagram.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Array;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_like")
public class Like {

    @EmbeddedId
    private LikeId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_profile_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne
    @MapsId("postId")
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
