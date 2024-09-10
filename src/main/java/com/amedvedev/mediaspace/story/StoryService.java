package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.story.exception.StoriesLimitReachedException;
import com.amedvedev.mediaspace.story.exception.StoryNotFoundException;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import com.amedvedev.mediaspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;

    public ViewStoryResponse createStory(CreateStoryRequest request) {
        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

        if (user.getStories().size() == 30) {
            throw new StoriesLimitReachedException("Maximum number of stories reached");
        }

        var media = Media.builder()
                .url(request.getCreateMediaRequest().getUrl())
                .build();

        var story = Story.builder()
                .user(user)
                .media(media)
                .build();

        user.getStories().add(story);

        userRepository.save(user);
        story.setId(user.getStories().getFirst().getId());

        return storyMapper.toViewStoryDto(story);
    }

    public List<ViewStoryResponse> getStoriesOfUser(String username) {
        var user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var stories = storyRepository.findByUserId(user.getId()).stream()
                .map(storyMapper::toViewStoryDto)
                .toList();

        if (stories.isEmpty()) {
            throw new StoryNotFoundException(String.format("User %s has no stories", username));
        }

        return stories;
    }

    public ViewStoryResponse getStoryById(Long id) {
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new StoryNotFoundException("Story not found"));

        return storyMapper.toViewStoryDto(story);
    }

    public void deleteStory(Long id) {
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new StoryNotFoundException("Story not found"));

        storyRepository.delete(story);
    }
}
