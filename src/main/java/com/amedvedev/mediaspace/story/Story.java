package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "story")
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "_user_id", nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        expiresAt = createdAt.plusDays(1);
    }
}
