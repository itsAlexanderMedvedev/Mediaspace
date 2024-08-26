package com.amedvedev.instagram.media;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "media")
public class Media {

    public enum MediaType {
        VIDEO, IMAGE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "url", nullable = false, length = 4096)
    private String url;

    @Column(name = "type", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private MediaType type;

    @Column(name = "resolution", nullable = false, length = 10)
    private String resolution;

    @Column(name = "format", length = 7, nullable = false)
    private String format;

    // in case of media being a video
    @Column(name = "duration_seconds")
    private int durationSeconds;  // name??

    @Builder.Default
    @OneToMany
    @JoinColumn(name = "media_id")
    private List<MediaPost> mediaPost = new ArrayList<>();

}
