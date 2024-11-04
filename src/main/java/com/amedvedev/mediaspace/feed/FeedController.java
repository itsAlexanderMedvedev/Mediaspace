package com.amedvedev.mediaspace.feed;

import com.amedvedev.mediaspace.post.PostService;
import com.amedvedev.mediaspace.story.dto.StoriesFeedEntry;
import com.amedvedev.mediaspace.story.service.StoryViewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feed")
@Tag(name = "Feed", description = "Endpoints for feed")
public class FeedController {

    private final PostService postService;
    private final StoryViewService storyViewService;

    @GetMapping("/stories")
    @ResponseStatus(HttpStatus.OK)
    public Set<StoriesFeedEntry> getStoriesFeed() {
        return storyViewService.getStoriesFeed();
    }

//    @GetMapping("/posts")
//    @ResponseStatus(HttpStatus.OK)
//    public List<ViewPostsFeedResponse> getPostsFeed() {
//        return postService.getStoriesFeed();
//    }
}
