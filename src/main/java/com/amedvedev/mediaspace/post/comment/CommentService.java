package com.amedvedev.mediaspace.post.comment;

import com.amedvedev.mediaspace.post.Post;
import com.amedvedev.mediaspace.post.PostService;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewCommentResponse;
import com.amedvedev.mediaspace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final PostService postService;
    private final UserService userService;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Transactional
    public ViewCommentResponse addComment(Long postId, AddCommentRequest request) {
        var post = postService.getPostById(postId);
        var user = userService.getCurrentUser();

        var comment = Comment.builder()
                .user(user)
                .body(request.getBody())
                .build();

        var savedComment = commentRepository.save(comment);
        return commentMapper.toViewCommentResponse(savedComment);
//        var comment = post.getComments().getLast();
//        return commentMapper.toViewCommentResponse(comment);
    }

}
