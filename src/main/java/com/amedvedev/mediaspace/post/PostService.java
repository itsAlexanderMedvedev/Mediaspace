package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import com.amedvedev.mediaspace.media.postmedia.PostMediaMapper;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final UserService userService;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostMediaMapper postMediaMapper;

    @Transactional
    public ViewPostResponse createPost(CreatePostRequest request) {
        var user = userService.getCurrentUser();
        log.info("Creating post for user: {}", user.getUsername());

        var post = buildPost(request, user);

        var savedPost = postRepository.save(post);
        var viewPostMediaResponseList = getViewPostMediaResponseList(savedPost);
        return postMapper.toViewPostResponse(savedPost, viewPostMediaResponseList);
    }

    private Post buildPost(CreatePostRequest request, User user) {
        log.debug("Building post from request: {}", request);
        var post = Post.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        var postMediaList = postMediaMapper.mapUrlsToPostMedia(request.getMediaUrls(), post);
        post.setPostMediaList(postMediaList);

        return post;
    }

    @Transactional(readOnly = true)
    public List<UserProfilePostResponse> getPostsOfUser(String username) {
        log.info("Fetching posts of user: {}", username);
        var user = userService.getUserDtoByUsername(username);
        var userProfilePostResponses = getUserProfilePostResponses(user);

        checkIfUserHasPosts(username, userProfilePostResponses);

        return userProfilePostResponses;
    }

    private void checkIfUserHasPosts(String username, List<UserProfilePostResponse> userProfilePostResponses) {
        if (userProfilePostResponses.isEmpty()) {
            log.warn("User: {} has no posts", username);
            throw new PostNotFoundException("User has no posts");
        }
    }

    private List<UserProfilePostResponse> getUserProfilePostResponses(UserDto user) {
        return findPostsByUserId(user.getId()).stream()
                .map(postMapper::toUserProfilePostResponse)
                .toList();
    }

    public List<Post> findPostsByUserId(Long id) {
        return postRepository.findAllByUserIdOrderByCreatedAt(id);
    }

    @Transactional(readOnly = true)
    public ViewPostResponse getViewPostResponseById(Long id) {
        log.info("Getting ViewPostResponse for post with id: {}", id);
        var post = findPostById(id);
        var viewPostMediaResponseList = getViewPostMediaResponseList(post);

        return postMapper.toViewPostResponse(post, viewPostMediaResponseList);
    }

    private List<ViewPostMediaResponse> getViewPostMediaResponseList(Post post) {
        return post.getPostMediaList().stream()
                .map(postMediaMapper::toViewPostMediaResponse)
                .toList();
    }

    public Post findPostById(Long id) {
        log.info("Fetching post by id: {}", id);
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));
    }

    @Transactional
    public void deletePostById(Long id) {
        log.info("Deleting post with id: {}", id);
        postRepository.deleteById(id);
    }

    @Transactional
    public void likePost(Long postId) {
        var post = findPostById(postId);
        var user = userService.getCurrentUser();

        log.info("User: {} is liking post with id: {}", postId, user.getUsername());

        post.addLike(buildLike(postId, user));

        postRepository.save(post);
    }

    private Like buildLike(Long postId, User user) {
        return Like.builder().id(new LikeId(user.getId(), postId)).user(user).build();
    }

    @Transactional
    public void unlikePost(Long postId) {
        var post = findPostById(postId);
        var user = userService.getCurrentUser();
        log.info("User: {} is unliking post with id: {}", postId, user.getUsername());

        removeLike(post, user);

        postRepository.save(post);
    }

    private void removeLike(Post post, User user) {
        var removed = post.getLikes().removeIf(like -> like.getUser().getId().equals(user.getId()));
        if (!removed) {
            log.warn("User: {} tried to unlike post that was not liked", user.getUsername());
            throw new PostNotLikedException("Cannot unlike post that was not liked");
        }
    }
}
