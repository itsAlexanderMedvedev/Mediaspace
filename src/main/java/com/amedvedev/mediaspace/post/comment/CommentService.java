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
import jakarta.validation.Valid;
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
        log.debug("Checking if post exists. PostId: {}", postId);
        var post = postService.getPostById(postId);

        log.debug("Fetching comments for postId: {}", postId);
        var comments = commentRepository.findAllByPostId(postId);

        log.info("Retrieved {} comments for postId: {}", comments.size(), postId);
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

    private void verifyCommentBelongsToUser(Comment comment) {
        System.out.println("service " + comment.getUser().getId() + " " + userService.getCurrentUser().getId());
        var currentUser = userService.getCurrentUser();
        if (!comment.getUser().getId().equals(currentUser.getId())) {
            log.warn("UserId: {} attempted to modify commentId: {} which does not belong to them",
                    currentUser.getId(), comment.getId());
            throw new ForbiddenActionException("You can only modify your own comments");
        }
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
    }

    public ViewCommentResponse addNestedComment(Long commentId, AddCommentRequest addCommentRequest) {
        log.debug("Attempting to add nested comment to commentId: {}, request: {}", commentId, addCommentRequest);

        var parentComment = findCommentById(commentId);

        var nestedComment = Comment.builder()
                .user(userService.getCurrentUser())
                .post(parentComment.getPost())
                .body(addCommentRequest.getBody())
                .build();
        parentComment.addNestedComment(nestedComment);

        var savedComment = commentRepository.save(parentComment);

        System.out.println("FROM SERVICE");
        System.out.println(commentRepository.findAll());
        System.out.println(savedComment.getNestedComments());

        log.info("Nested comment added successfully. CommentId: {}, parentCommentId: {}, userId: {}",
                savedComment.getId(), commentId, userService.getCurrentUser().getId());

        return commentMapper.toViewCommentResponse(savedComment);
    }
}
