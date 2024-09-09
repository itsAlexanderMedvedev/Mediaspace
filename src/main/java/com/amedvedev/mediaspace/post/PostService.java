package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.media.postmedia.PostMedia;
import com.amedvedev.mediaspace.media.postmedia.PostMediaId;
import com.amedvedev.mediaspace.post.comment.Comment;
import com.amedvedev.mediaspace.post.comment.dto.AddCommentRequest;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
import com.amedvedev.mediaspace.post.comment.exception.CommentNotFoundException;
import com.amedvedev.mediaspace.post.dto.CreatePostRequest;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
import com.amedvedev.mediaspace.post.exception.PostNotFoundException;
import com.amedvedev.mediaspace.post.like.Like;
import com.amedvedev.mediaspace.post.like.LikeId;
import com.amedvedev.mediaspace.user.UserRepository;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    public ViewPostResponse createPost(CreatePostRequest request) {
         var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

        var post = Post.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        var postMediaList = IntStream.range(0, request.getMediaUrls().size())
                .mapToObj(index -> {
                    var url = request.getMediaUrls().get(index);
                    var media = Media.builder().url(url).build();
                    var mediaPostId = new PostMediaId(media.getId(), index + 1);
                    return new PostMedia(mediaPostId, media, post);
                })
                .toList();

        post.setPostMediaList(postMediaList);

        postRepository.save(post);

        return postMapper.toViewPostResponse(post);
    }

    public List<ViewPostResponse> getPostsOfUser(String username) {
        var user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var posts = postRepository.findAllByUserIdOrderByCreatedAt(user.getId());

        return posts.stream()
                .map(postMapper::toViewPostResponse)
                .toList();
    }

    public ViewPostResponse getPostById(Long id) {
        var post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        return postMapper.toViewPostResponse(post);
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    public void addComment(Long postId, AddCommentRequest request) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

        var comment = Comment.builder()
                .user(user)
                .post(post)
                .body(request.getBody())
                .build();

        post.getComments().add(comment);

        postRepository.save(post);
    }

    public ViewPostCommentsResponse getComments(Long postId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        return postMapper.toViewPostCommentsResponse(post);
    }

    public void likePost(Long postId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

        post.getLikes().add(Like.builder().id(new LikeId(user.getId(), postId)).post(post).user(user).build());

        postRepository.save(post);
    }

    public void deleteComment(Long postId, Long commentId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        var comment = post.getComments().stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));

        if (!comment.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
            throw new IllegalArgumentException("You can only delete your own comments");
        }

        post.getComments().remove(comment);

        postRepository.save(post);
    }
}
