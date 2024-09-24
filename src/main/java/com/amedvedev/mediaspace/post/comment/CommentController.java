package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.amedvedev.mediaspace.exception.dto.ValidationErrorResponse;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewCommentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
@Tag(name = "Comment", description = "Comment API")
public class CommentController {

    private final CommentService commentService;

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
    @PostMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ViewCommentResponse addComment(@PathVariable Long postId, @RequestBody AddCommentRequest addCommentRequest) {
        return commentService.addComment(postId, addCommentRequest);
    }

//    @PutMapping("/{commentId}")
//    @ResponseStatus(HttpStatus.OK)
//    public ViewCommentResponse editComment(@PathVariable Long commentId, @RequestBody CommentDto commentDto) {
//        return commentService.editComment(commentId, commentDto);
//    }
//
//    @GetMapping("/posts/{postId}")
//    @ResponseStatus(HttpStatus.OK)
//    public ResponseEntity<List<CommentDto>> getCommentsByPost(@PathVariable Long postId) {
//        List<CommentDto> comments = commentService.getCommentsByPostId(postId);
//        return ResponseEntity.ok(comments);
//    }
//
//    @DeleteMapping("/{commentId}")
//    @ResponseStatus(HttpStatus.NO_CONTENT)
//    public void deleteComment(@PathVariable Long commentId) {
//        commentService.deleteComment(commentId);
//    }
}
