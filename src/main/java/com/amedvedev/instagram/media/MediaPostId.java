package com.amedvedev.instagram.media;

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
public class MediaPostId implements Serializable {

    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "position")
    private int position;
}
