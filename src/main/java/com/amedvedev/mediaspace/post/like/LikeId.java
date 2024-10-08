package com.amedvedev.mediaspace.post.like;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class LikeId implements Serializable {

    @Column(name = "_user_id")
    private Long userId;

    @Column(name = "post_id")
    private Long postId;
}
