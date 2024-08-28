package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.story.dto.CreateStoryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Story", description = "Endpoints for managing stories")
@RestController
@RequestMapping("/api/story")
public class StoryController {

    @Operation(summary = "Create a new story", description = "Creates a new story for a user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Story created successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400", description = "Invalid input",
                    content = @Content
            )
    })
    @PostMapping
    public void createStory(@RequestBody CreateStoryDto request) {

    }
}
