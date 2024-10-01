package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.amedvedev.mediaspace.exception.dto.ValidationErrorResponse;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.EditCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewCommentResponse;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
@Tag(name = "Comment", description = "Endpoints for managing comments")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Add a comment to a post", description = "Adds a comment to a post.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", description = "Comment added successfully",
                    content = @Content(schema = @Schema(implementation = ViewCommentResponse.class))
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
    @PostMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ViewCommentResponse addComment(@PathVariable Long postId,
                                          @Valid @RequestBody AddCommentRequest addCommentRequest) {
        return commentService.addComment(postId, addCommentRequest);
    }

    @PostMapping("/{commentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ViewCommentResponse addNestedComment(@PathVariable Long commentId,
                                                @Valid @RequestBody AddCommentRequest addCommentRequest) {
        return commentService.addNestedComment(commentId, addCommentRequest);
    }

    @Operation(summary = "Edit a comment")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Comment edited successfully",
                    content = @Content(schema = @Schema(implementation = ViewCommentResponse.class))
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
                    responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Comment not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public ViewCommentResponse editComment(@PathVariable Long commentId,
                                           @Valid @RequestBody EditCommentRequest editCommentRequest) {

        return commentService.editComment(commentId, editCommentRequest);
    }

    @Operation(summary = "Get comments by post ID")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Comments retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ViewPostCommentsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Post not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @GetMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.OK)
    public ViewPostCommentsResponse getCommentsByPost(@PathVariable Long postId) {
        return commentService.getCommentsByPostId(postId);
    }

    @Operation(summary = "Delete a comment")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204", description = "Comment deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "Comment not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
    }
}
