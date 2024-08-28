package com.amedvedev.mediaspace.media;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "url", nullable = false, length = 4096)
    private String url;

    @Builder.Default
    @OneToMany
    @JoinColumn(name = "media_id")
    private List<MediaPost> mediaPost = new ArrayList<>();

}
