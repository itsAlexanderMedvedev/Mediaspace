package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import com.amedvedev.mediaspace.media.postmedia.PostMedia;
import com.amedvedev.mediaspace.media.postmedia.PostMediaId;
import com.amedvedev.mediaspace.post.comment.CommentService;
import com.amedvedev.mediaspace.post.comment.dto.ViewPostCommentsResponse;
import com.amedvedev.mediaspace.post.dto.CreatePostRequest;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
import com.amedvedev.mediaspace.post.exception.PostNotFoundException;
import com.amedvedev.mediaspace.post.like.Like;
import com.amedvedev.mediaspace.post.like.LikeId;
import com.amedvedev.mediaspace.post.like.exception.PostNotLikedException;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserService;
import com.amedvedev.mediaspace.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final PostMapper postMapper;
    private final CommentService commentService;

    @Transactional
    public ViewPostResponse createPost(CreatePostRequest request) {
        var user = userService.getCurrentUser();
        var post = buildPost(request, user);

        var savedPost = postRepository.save(post);
        return postMapper.toViewPostResponse(savedPost);
    }

    private Post buildPost(CreatePostRequest request, User user) {
        var post = Post.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        var postMediaList = mapUrlsToPostMedia(request.getMediaUrls(), post);
        post.setPostMediaList(postMediaList);

        return post;
    }

    private List<PostMedia> mapUrlsToPostMedia(List<CreateMediaRequest> createMediaRequests, Post post) {
        return IntStream.range(0, createMediaRequests.size())
                .mapToObj(index -> {
                    var url = createMediaRequests.get(index).getUrl();
                    var media = Media.builder().url(url).build();
                    var mediaPostId = new PostMediaId(media.getId(), index + 1);
                    return new PostMedia(mediaPostId, media, post);
                })
                .toList();
    }

    public Post savePost(Post post) {
        return postRepository.save(post);
    }

    @Transactional(readOnly = true)
    public List<UserProfilePostResponse> getPostsOfUser(String username) {
        var user = userService.getUserDtoByUsername(username);
        return getUserProfilePostResponses(user);
    }

    private List<UserProfilePostResponse> getUserProfilePostResponses(UserDto user) {
        return getPostsByUserId(user.getId()).stream()
                .map(postMapper::toUserProfilePostResponse)
                .toList();
    }

    public List<Post> getPostsByUserId(Long id) {
        return postRepository.findAllByUserIdOrderByCreatedAt(id);
    }

    @Transactional(readOnly = true)
    public ViewPostResponse getViewPostResponseById(Long id) {
        var post = getPostById(id);
        return postMapper.toViewPostResponse(post);
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));
    }

    @Transactional
    public void deletePostById(Long id) {
        postRepository.deleteById(id);
    }

//    @Transactional
//    public void addComment(Long postId, AddCommentRequest request) {
//        var post = getPostById(postId);
//        addCommentToPost(request.getBody(), post);
//        postRepository.save(post);
//    }

//    private void addCommentToPost(String commentBody, Post post) {
//        var user = userService.getCurrentUser();
//
//        var comment = Comment.builder()
//                .user(user)
//                .body(commentBody)
//                .build();
//
//        post.addComment(comment);
//    }

    @Transactional(readOnly = true)
    public ViewPostCommentsResponse getComments(Long postId) {
        var post = getPostById(postId);
        return postMapper.toViewPostCommentsResponse(post);
    }

    @Transactional
    public void likePost(Long postId) {
        var post = getPostById(postId);
        var user = userService.getCurrentUser();

        post.addLike(buildLike(postId, user));

        postRepository.save(post);
    }

    private Like buildLike(Long postId, User user) {
        return Like.builder().id(new LikeId(user.getId(), postId)).user(user).build();
    }

//    @Transactional
//    public void deleteComment(Long postId, Long commentId) {
//        var comment = getCommentById(commentId);
//
//        verifyCommentBelongsToPost(postId, comment);
//        verifyCommentBelongsToUser(comment);
//
//        postRepository.save(post);
//    }

//    private Comment getCommentById(Long commentId) {
//        return commentRepository.findById(commentId)
//                .orElseThrow(() -> new CommentNotFoundException("Comment not found"));
//    }
//
//    private void verifyCommentBelongsToPost(Long postId, Comment comment) {
//        if (!comment.getPost().getId().equals(postId)) {
//           throw new CommentNotFoundException("Comment not found in the specified post");
//        }
//    }
//
//    private void verifyCommentBelongsToUser(Comment comment) {
//        if (!comment.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
//            throw new ForbiddenActionException("Cannot delete comment of another user");
//        }
//    }

    @Transactional
    public void unlikePost(Long postId) {
        var post = getPostById(postId);

        var user = userService.getCurrentUser();

        var removed = post.getLikes().removeIf(like -> like.getUser().getId().equals(user.getId()));

        if (!removed) {
            throw new PostNotLikedException("Cannot unlike post that was not liked");
        }

        postRepository.save(post);
    }
}
