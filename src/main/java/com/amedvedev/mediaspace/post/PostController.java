package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.amedvedev.mediaspace.exception.dto.ValidationErrorResponse;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
import com.amedvedev.mediaspace.post.dto.CreatePostRequest;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
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
@RequestMapping("/api/posts")
@Tag(name = "Post", description = "Endpoints for managing posts")
public class PostController {

    private final PostService postService;

    @Operation(summary = "Create a new post. (max 30 per user)", description = "Creates a new post for a user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Post created successfully",
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
    public ViewPostResponse createPost(@Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(request);
    }

    @Operation(summary = "Get all posts for a user", description = "Returns all posts for a user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Posts found",
                    content = @Content(schema = @Schema(implementation = ViewPostResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Posts not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @GetMapping("/user/{username}")
    @ResponseStatus(HttpStatus.OK)
    public List<ViewPostResponse> getPostsOfUser(@PathVariable String username) {
        return postService.getPostsOfUser(username);
    }


    @Operation(summary = "Get a post by ID", description = "Returns a post by its ID.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Post found",
                    content = @Content(schema = @Schema(implementation = ViewPostResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ViewPostResponse getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @Operation(summary = "Add a comment to a post", description = "Adds a comment to a post.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", description = "Comment added successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public void addCommentToPost(@PathVariable Long postId, @RequestBody AddCommentRequest request) {
        postService.addComment(postId, request);
    }

    @Operation(summary = "Get comments of a post", description = "Returns comments of a post.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Comments found",
                    content = @Content(schema = @Schema(implementation = ViewPostResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Comments not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @GetMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.OK)
    public ViewPostCommentsResponse getCommentsOfPost(@PathVariable Long postId) {
        return postService.getComments(postId);
    }

    public void deleteComment(Long postId, Long commentId) {
        postService.deleteComment(postId, commentId);
    }

    @Operation(summary = "Like a post", description = "Likes a post.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Post liked successfully",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @PutMapping("/{postId}/likes")
    @ResponseStatus(HttpStatus.OK)
    public void likePost(@PathVariable Long postId) {
        postService.likePost(postId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable Long id) {
        postService.deletePost(id);
    }
}


