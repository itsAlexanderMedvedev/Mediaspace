package com.amedvedev.instagram.post.like;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class LikeId implements Serializable {

    @Column(name = "_user_id")
    private Long userId;

    @Column(name = "post_id")
    private Long postId;
}
