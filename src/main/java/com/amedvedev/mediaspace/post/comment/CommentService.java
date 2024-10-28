package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.exception.ForbiddenActionException;
import com.amedvedev.mediaspace.post.PostRepository;
import com.amedvedev.mediaspace.post.PostService;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.EditCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewCommentResponse;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
import com.amedvedev.mediaspace.post.comment.exception.CommentNotFoundException;
import com.amedvedev.mediaspace.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final PostService postService;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final PostRepository postRepository;

    @Transactional
    public ViewCommentResponse addComment(Long postId, AddCommentRequest request) {
        log.info("Adding comment to a post with postId: {}", postId);

        var post = postService.findPostById(postId);
        var user = userService.getCurrentUser();
        var body = request.getBody();

        var comment = Comment.builder()
                .user(user)
                .post(post)
                .body(body)
                .build();

        var savedComment = commentRepository.save(comment);

        return commentMapper.toViewCommentResponse(savedComment);
    }

    @Transactional
    public ViewCommentResponse editComment(Long commentId, EditCommentRequest editCommentRequest) {
        log.info("Editing comment with commentId: {}", commentId);

        var comment = findCommentById(commentId);
        var newCommentBody = editCommentRequest.getUpdatedBody();

        verifyCommentBelongsToUser(comment);

        if (newCommentBody.equals(comment.getBody())) {
            log.debug("No changes detected in commentId: {}. Skipping update.", commentId);
            return commentMapper.toViewCommentResponse(comment);
        }

        comment.setBody(newCommentBody);
        var updatedComment = commentRepository.save(comment);

        return commentMapper.toViewCommentResponse(updatedComment);
    }

    @Transactional(readOnly = true)
    public ViewPostCommentsResponse getCommentsByPostId(Long postId) {
        log.info("Fetching comments for postId: {}", postId);

        log.debug("Checking if post exists. PostId: {}", postId);
        var post = postService.findPostById(postId);

        log.debug("Fetching comments for postId: {}", postId);
        var comments = commentRepository.findAllByPostId(postId);

        checkIfPostHasComments(postId, comments);

        return commentMapper.toViewPostCommentsResponse(comments, postId);
    }

    private void checkIfPostHasComments(Long postId, List<Comment> comments) {
        if (comments.isEmpty()) {
            log.warn("No comments found for postId: {}", postId);
            throw new CommentNotFoundException("No comments found for post");
        }
    }

    @Transactional
    public void deleteComment(Long commentId) {
        log.info("Deleting comment with id: {}", commentId);
        var comment = findCommentById(commentId);

        verifyCommentBelongsToUser(comment);

        comment.setDeleted(true);
        commentRepository.save(comment);
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
        log.info("Fetching comment by commentId: {}", commentId);
        return commentRepository.findById(commentId)
                .orElseThrow(() -> {
                    log.warn("Comment not found with id: {}", commentId);
                    return new CommentNotFoundException("Comment not found");
                });
    }

    @Transactional
    public ViewCommentResponse addNestedComment(Long commentId, AddCommentRequest addCommentRequest) {
        log.info("Adding a nested comment to comment with id: {}", commentId);

        var parentComment = findCommentById(commentId);

        var nestedComment = Comment.builder()
                .user(userService.getCurrentUser())
                .body(addCommentRequest.getBody())
                .build();
        parentComment.addNestedComment(nestedComment);

        var savedComment = commentRepository.save(parentComment);

        return commentMapper.toViewCommentResponse(savedComment);
    }
}
