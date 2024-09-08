package com.amedvedev.mediaspace.media.postmedia;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.post.Post;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "post_media")
public class PostMedia {

    @EmbeddedId
    private PostMediaId id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @MapsId("mediaId")
    @JoinColumn(name = "media_id", insertable = false, updatable = false)
    private Media media;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
}


