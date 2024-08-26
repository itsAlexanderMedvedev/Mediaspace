package com.amedvedev.instagram.user;

import com.amedvedev.instagram.user.dto.UpdateUserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/{username}")
    public ResponseEntity<Map<String, String>> updateUserInfo(@PathVariable String username, @Valid @RequestBody UpdateUserDto updateUserDto) {

        userService.updateUser(username, updateUserDto);
        return ResponseEntity.status(HttpStatus.OK).body(
                new HashMap<>() {{
                    put("message", "Please, log in again with your new credentials");
                }}
        );
    }
}
