package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.exception.BadRequestActionException;
import com.amedvedev.mediaspace.exception.ForbiddenActionException;
import com.amedvedev.mediaspace.post.PostService;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.EditCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewCommentResponse;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
import com.amedvedev.mediaspace.post.comment.exception.CommentNotFoundException;
import com.amedvedev.mediaspace.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final PostService postService;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public ViewCommentResponse addComment(Long postId, AddCommentRequest request) {
        log.debug("Attempting to add comment to postId: {}, request: {}", postId, request);

        var post = postService.getPostById(postId);
        var user = userService.getCurrentUser();
        var body = request.getBody();

        verifyCommentBodyIsNotBlank(body);

        var comment = Comment.builder()
                .user(user)
                .post(post)
                .body(body)
                .build();

        var savedComment = commentRepository.save(comment);

        log.info("Comment added successfully. CommentId: {}, postId: {}, userId: {}",
                savedComment.getId(), postId, user.getId());

        return commentMapper.toViewCommentResponse(savedComment);
    }

    @Transactional
    public ViewCommentResponse editComment(Long commentId, EditCommentRequest editCommentRequest) {
        log.debug("Attempting to edit commentId: {}, request: {}", commentId, editCommentRequest);

        var comment = findCommentById(commentId);
        var newCommentBody = editCommentRequest.getUpdatedBody();

        verifyCommentBelongsToUser(comment);
        verifyCommentBodyIsNotBlank(newCommentBody);

        if (newCommentBody.equals(comment.getBody())) {
            log.info("No changes detected in commentId: {}. Skipping update.", commentId);
            return commentMapper.toViewCommentResponse(comment);
        }

        comment.setBody(newCommentBody);
        var updatedComment = commentRepository.save(comment);

        log.info("Comment updated successfully. CommentId: {}", updatedComment.getId());

        return commentMapper.toViewCommentResponse(updatedComment);
    }

    @Transactional(readOnly = true)
    public ViewPostCommentsResponse getCommentsByPostId(Long postId) {
        log.debug("Fetching comments for postId: {}", postId);

        var comments = commentRepository.findAllByPostId(postId);

        log.info("Retrieved {} comments for postId: {}", Integer.valueOf(comments.size()), postId);

        return commentMapper.toViewPostCommentsResponse(comments, postId);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        log.debug("Attempting to delete commentId: {}", commentId);

        var comment = findCommentById(commentId);

        verifyCommentBelongsToUser(comment);

        comment.setDeleted(true);
        commentRepository.save(comment);

        log.info("Comment deleted successfully. CommentId: {}", commentId);
    }

    private void verifyCommentBodyIsNotBlank(String commentBody) {
        if (commentBody == null || commentBody.isBlank()) {
            log.warn("Comment body is blank or null.");
            throw new BadRequestActionException("Comment body cannot be empty");
        }
    }

    private void verifyCommentBelongsToUser(Comment comment) {
        var currentUser = userService.getCurrentUser();
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            log.warn("UserId: {} attempted to modify commentId: {} which does not belong to them",
                    currentUser.getId(), comment.getId());
            throw new ForbiddenActionException("You can only modify your own comments");
        }
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.error("Comment not found. CommentId: {}", commentId);
                    return new CommentNotFoundException("Comment not found");
                });
    }
}
