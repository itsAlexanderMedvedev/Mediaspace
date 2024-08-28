package com.amedvedev.mediaspace.media;

import com.amedvedev.mediaspace.post.Post;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "media_post")
public class MediaPost {

    @EmbeddedId
    private MediaPostId id;

    @ManyToOne
    @MapsId("mediaId")
    @JoinColumn(name = "media_id", insertable = false, updatable = false)
    private Media media;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

}
