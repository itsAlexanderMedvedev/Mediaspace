package com.amedvedev.mediaspace.post;

import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.media.PostMedia;
import com.amedvedev.mediaspace.media.PostMediaId;
import com.amedvedev.mediaspace.post.dto.CreatePostRequest;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
import com.amedvedev.mediaspace.post.exception.PostNotFoundException;
import com.amedvedev.mediaspace.user.UserRepository;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    public ViewPostResponse createPost(CreatePostRequest request) {
        var user = userRepository.findByUsernameIgnoreCase(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var post = Post.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();

        var postMediaList = IntStream.range(0, request.getMediaUrls().size())
                .mapToObj(index -> {
                    var url = request.getMediaUrls().get(index);
                    var media = Media.builder().url(url).build();
                    var mediaPostId = new PostMediaId(media.getId(), index);
                    return new PostMedia(mediaPostId, media, post);
                })
                .collect(Collectors.toList());

        post.setPostMediaList(postMediaList);

        postRepository.save(post);

        return postMapper.toViewPostResponse(post);
    }

    public ViewPostResponse getPostById(Long id) {
        var post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("Post not found"));

        return postMapper.toViewPostResponse(post);
    }

    public List<ViewPostResponse> getPostsOfUser(String username) {
        var user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var posts = postRepository.findAllByUserId(user.getId());

        return posts.stream()
                .map(postMapper::toViewPostResponse)
                .collect(Collectors.toList());
    }

    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }
}
