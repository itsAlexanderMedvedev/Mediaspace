package com.amedvedev.mediaspace.user.follow;

import com.amedvedev.mediaspace.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "follow")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Follow {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private FollowId id;

    @ManyToOne
    @MapsId("followerId")
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne
    @MapsId("followeeId")
    @JoinColumn(name = "followee_id")
    private User followee;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
