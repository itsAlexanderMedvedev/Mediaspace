package com.amedvedev.mediaspace.post.like;

import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.user.User;
import jakarta.persistence.*;
import lombok.*;

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
    @JoinColumn(name = "_user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne
    @MapsId("postId")
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
