package com.amedvedev.mediaspace.media;

import com.amedvedev.mediaspace.post.Post;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_media")
public class PostMedia {

    @EmbeddedId
    private PostMediaId id;

    @ManyToOne
    @MapsId("mediaId")
    @JoinColumn(name = "media_id", insertable = false, updatable = false)
    private Media media;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
}
