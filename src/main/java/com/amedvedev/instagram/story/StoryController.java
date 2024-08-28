package com.amedvedev.instagram.story;

import com.amedvedev.instagram.story.dto.CreateStoryDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/story")
public class StoryController {

    @PostMapping
    public void createStory(@RequestBody CreateStoryDto request) {

    }
}
