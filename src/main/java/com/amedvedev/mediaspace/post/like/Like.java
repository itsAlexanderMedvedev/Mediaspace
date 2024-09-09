package com.amedvedev.mediaspace.post.like;

import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "_like")
@SQLRestriction(value = "is_deleted<>'TRUE'")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Like {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private LikeId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "_user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne
    @MapsId("postId")
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private Post post;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

}


