package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.post.comment.Comment;
import com.amedvedev.mediaspace.post.like.Like;
import com.amedvedev.mediaspace.story.Story;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "_user")
@ToString(onlyExplicitlyIncluded = true)
@SQLRestriction(value = "is_deleted<>'TRUE'")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements UserDetails {

    @Id
    @ToString.Include
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_picture_id")
    private Media profilePicture;

    @ToString.Include
    @Column(name = "username", nullable = false, length = 20, unique = true)
    private String username;

    @Column(name = "password", nullable = false, length = 64)
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "user", orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @Builder.Default
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "user", orphanRemoval = true)
    private List<Story> stories = new ArrayList<>();

    @Builder.Default
    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "follow",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "followee_id")
    )
    private List<User> following = new ArrayList<>();

    @Builder.Default
    @ManyToMany(mappedBy = "following")
    private List<User> followers = new ArrayList<>();

    @Builder.Default
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Like> likes = new ArrayList<>();

    @Builder.Default
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "user")
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "user_tag",
            joinColumns = @JoinColumn(name = "_user_id"),
            inverseJoinColumns = @JoinColumn(name = "post_id")
    )
    private List<Post> taggedAt = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return !isDeleted;
    }

    public void follow(User user) {
        following.add(user);
        user.followers.add(this);
    }

    public void unfollow(User user) {
        following.remove(user);
        user.followers.remove(this);
    }
    
    public String getProfilePictureUrl() {
        return profilePicture == null ? null : profilePicture.getUrl();
    }
}


