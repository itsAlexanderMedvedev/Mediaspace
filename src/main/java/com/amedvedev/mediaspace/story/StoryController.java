package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.amedvedev.mediaspace.exception.dto.ValidationErrorResponse;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.dto.StoryPreviewResponse;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import com.amedvedev.mediaspace.story.service.StoryManagementService;
import com.amedvedev.mediaspace.story.service.StoryViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stories")
@Tag(name = "Story", description = "Endpoints for managing stories")
public class StoryController {

    private final StoryManagementService storyManagementService;
    private final StoryViewService storyViewService;

    @Operation(summary = "Create a new story. (max 30 per user)")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Story created successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoryDto createStory(@Valid @RequestBody CreateStoryRequest request) {
        return storyManagementService.createStory(request);
    }

    @Operation(summary = "Get a story by ID")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Story found",
                    content = @Content(schema = @Schema(implementation = StoryDto.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Story not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ViewStoryResponse getStoryById(@PathVariable Long id) {
        return storyViewService.getViewStoryResponseByStoryId(id);
    }

    @Operation(summary = "Get all stories of a user")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Stories found",
                    content = @Content(schema = @Schema(implementation = StoryDto.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User has no stories",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @GetMapping("/user/{username}")
    @ResponseStatus(HttpStatus.OK)
    public List<StoryPreviewResponse> getStoriesOfUser(@PathVariable String username) {
        return storyViewService.getStoryPreviewsOfUser(username);
    }

    @Operation(summary = "Get all stories of the current user")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Stories found",
                    content = @Content(schema = @Schema(implementation = StoryDto.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User has no stories",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public List<StoryPreviewResponse> getStoriesOfCurrentUser() {
        return storyViewService.getCurrentUserStories();
    }

    @Operation(summary = "Delete a story")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204", description = "Story deleted"
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Story not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStory(@PathVariable Long id) {
        storyManagementService.deleteStory(id);
    }
}

