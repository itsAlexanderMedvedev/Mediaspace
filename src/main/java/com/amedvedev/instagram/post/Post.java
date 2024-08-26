package com.amedvedev.instagram.post;

import com.amedvedev.instagram.media.Media;
import com.amedvedev.instagram.media.MediaPost;
import com.amedvedev.instagram.media.MediaPostId;
import com.amedvedev.instagram.post.comment.Comment;
import com.amedvedev.instagram.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "_user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 64)
    private String title;

    @Column(name = "description", length = 2048)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_like",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "_user_id")
    )
    private Set<User> likedByUsers = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.position ASC")
    private List<MediaPost> mediaPosts = new ArrayList<>();
}
