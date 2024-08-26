package com.amedvedev.instagram;

import com.amedvedev.instagram.media.Media;
import com.amedvedev.instagram.user.User;
import com.amedvedev.instagram.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InstagramApplication {

    public static void main(String[] args) {
        SpringApplication.run(InstagramApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(UserRepository userRepository) {
        return clr -> {
            userRepository.save(User.builder()
                            .username("user1")
                            .password("password1")
                            .build()
            );
        };
    }

}
