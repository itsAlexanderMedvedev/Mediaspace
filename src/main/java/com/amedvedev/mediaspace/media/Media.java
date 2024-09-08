package com.amedvedev.mediaspace.media;

import com.amedvedev.mediaspace.media.postmedia.PostMedia;
import jakarta.persistence.*;
import lombok.*;

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

    @OneToOne(mappedBy = "media")
    @JoinColumn(name = "media_id")
    private PostMedia postMedia;

}
