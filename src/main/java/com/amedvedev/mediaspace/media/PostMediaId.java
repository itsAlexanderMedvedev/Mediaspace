package com.amedvedev.mediaspace.media;

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
public class PostMediaId implements Serializable {

    @Column(name = "media_id")
    private Long mediaId;

    @Column(name = "position")
    private Integer position;
}
