package com.amedvedev.mediaspace.user.dto;

import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ViewUserProfileResponse {

    @EqualsAndHashCode.Include
    @Schema(description = "Username", example = "username")
    private String username;

    @Schema(description = "Profile picture URL", example = "https://example.com/profile.jpg")
    private String profilePictureUrl;

    @Schema(description = "User's posts")
    private List<UserProfilePostResponse> posts;

    @Schema(description = "User's stories IDs")
    private List<Long> storiesIds;

    @Schema(description = "Followers count", example = "100")
    private long followersCount;

    @Schema(description = "Following count", example = "100")
    private long followingCount;
}
