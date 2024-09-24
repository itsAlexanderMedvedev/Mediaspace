package com.amedvedev.mediaspace.media;

import com.amedvedev.mediaspace.media.postmedia.PostMedia;
import com.amedvedev.mediaspace.story.Story;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "media")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Media {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url", length = 4096)
    private String url;

    @OneToOne(mappedBy = "media")
    @JoinColumn(name = "media_id")
    private PostMedia postMedia;

    @OneToOne(mappedBy = "media")
    private Story story;

}
