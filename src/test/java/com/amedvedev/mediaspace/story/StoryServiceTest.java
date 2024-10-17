package com.amedvedev.mediaspace.story;

import ch.qos.logback.classic.Level;
import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.dto.StoryPreviewResponse;
import com.amedvedev.mediaspace.story.event.StoryCreatedEvent;
import com.amedvedev.mediaspace.story.exception.StoriesLimitReachedException;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserService;
import com.amedvedev.mediaspace.user.dto.UserDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ch.qos.logback.classic.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class StoryServiceTest {

    @InjectMocks
    private StoryService storyService;

    @Mock
    private UserService userService;

    @Mock
    private StoryRepository storyRepository;

    @Mock
    private StoryMapper storyMapper;

    @Mock
    private StoryRedisService storyRedisService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MockitoSession mockitoSession;

    @BeforeAll
    public static void beforeAll() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.DEBUG);
    }

    @BeforeEach
    public void setUp() {
        mockitoSession = Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.STRICT_STUBS)
                .startMocking();
    }

    @AfterEach
    public void tearDown() {
        mockitoSession.finishMocking();
    }

    @Test
    void shouldCreateStory() {
        CreateStoryRequest request = createStoryRequest();
        User user = new User();
        Story story = new Story();
        StoryDto storyDto = new StoryDto();

        when(userService.getCurrentUser()).thenReturn(user);
        when(storyRepository.save(any(Story.class))).thenReturn(story);
        when(storyMapper.toStoryDto(story)).thenReturn(storyDto);

        StoryDto result = storyService.createStory(request);

        verify(storyRepository).save(any(Story.class));
        verify(eventPublisher).publishEvent(any(StoryCreatedEvent.class));
        assertThat(storyDto).isEqualTo(result);
    }

    @Test
    void shouldNotCreateStoryIfMaximumStoriesCountIsReached() {
        CreateStoryRequest request = mock(CreateStoryRequest.class);
        User user = new User();

        when(userService.getCurrentUser()).thenReturn(user);
        fillUserWithEmptyStories(user, storyService.getMaximumStoriesCount());

        assertThatThrownBy(() -> storyService.createStory(request))
                .isInstanceOf(StoriesLimitReachedException.class)
                .hasMessage("Maximum number of stories reached");

        verify(storyRepository, never()).save(any(Story.class));
        verify(eventPublisher, never()).publishEvent(any(StoryCreatedEvent.class));
    }

    private void fillUserWithEmptyStories(User user, int storiesCount) {
        var stories = new ArrayList<Story>();
        for (int i = 0; i < storiesCount; i++) {
            stories.add(new Story());
        }
        user.setStories(stories);
    }

    @Test
    void shouldReturnStoryPreviewsFromDbWhenCacheIsEmpty() {
        var username = "username";
        var userId = 1L;
        var user = UserDto.builder().id(userId).username(username).build();
        var stories = List.of(new Story(), new Story());
        var expectedResponses = List.of(new StoryPreviewResponse(), new StoryPreviewResponse());

        when(userService.getUserDtoByUsername(username)).thenReturn(user);
        when(storyRedisService.getStoriesIdsForUser(userId)).thenReturn(List.of()); // Simulate empty cache
        when(storyRepository.findByUserId(anyLong())).thenReturn(stories);
        when(storyMapper.toStoryPreviewResponse(any(Story.class))).thenReturn(expectedResponses.get(0), expectedResponses.get(1));

        var result = storyService.getStoryPreviewsOfUser(username);

        assertThat(result).isEqualTo(expectedResponses);
        verify(storyRedisService).cacheStoriesIdsForUser(userId, stories);
    }

    @Test
    void shouldReturnStoryPreviewsFromCache() {
        var username = "username";
        var userId = 1L;
        var user = UserDto.builder().id(userId).username(username).build();
        var storiesIds = List.of(1L, 2L);
        var expectedResponses = List.of(new StoryPreviewResponse(), new StoryPreviewResponse());

        when(userService.getUserDtoByUsername(username)).thenReturn(user);
        when(storyRedisService.getStoriesIdsForUser(userId)).thenReturn(storiesIds);
        when(storyRedisService.getStoryDtoById(anyLong())).thenReturn(Optional.of(new StoryDto()));
        when(storyMapper.toStoryPreviewResponse(any(StoryDto.class))).thenReturn(expectedResponses.get(0), expectedResponses.get(1));

        var result = storyService.getStoryPreviewsOfUser(username);

        assertThat(result).isEqualTo(expectedResponses);
    }

    @Test
    void shouldReturnStoryPreviewsFromCache() {
        var username = "username";
        var userId = 1L;
        var user = UserDto.builder().id(userId).username(username).build();
        var storiesIds = List.of(1L, 2L);
        var expectedResponses = List.of(new StoryPreviewResponse(), new StoryPreviewResponse());

        when(userService.getUserDtoByUsername(username)).thenReturn(user);
        when(storyRedisService.getStoriesIdsForUser(userId)).thenReturn(storiesIds);
        when(storyRedisService.getStoryDtoById(anyLong())).thenReturn(Optional.of(new StoryDto()));
        when(storyMapper.toStoryPreviewResponse(any(StoryDto.class))).thenReturn(expectedResponses.get(0), expectedResponses.get(1));

        var result = storyService.getStoryPreviewsOfUser(username);

        assertThat(result).isEqualTo(expectedResponses);
    }

    @Test
    void getStoryPreviewsOfUser() {
        User user = new User();

    }

    @Test
    void getCurrentUserStories() {
    }

    @Test
    void getStoriesByUserId() {
    }

    @Test
    void getViewStoryResponseByStoryId() {
    }

    @Test
    void deleteStory() {
    }

    @Test
    void getStoriesFeed() {
    }

    private CreateStoryRequest createStoryRequest() {
        CreateMediaRequest mediaRequest = mock(CreateMediaRequest.class);
        CreateStoryRequest request = mock(CreateStoryRequest.class);

        when(mediaRequest.getUrl()).thenReturn("https://example.com/media");
        when(request.getCreateMediaRequest()).thenReturn(mediaRequest);

        return request;
    }
}