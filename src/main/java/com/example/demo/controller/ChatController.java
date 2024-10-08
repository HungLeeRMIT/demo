package com.example.demo.controller;

import com.example.demo.service.OpenAIService;
import com.example.demo.service.UserService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ChatController {

    private final OpenAIService openAIService;
    private final UserService userService;

    public ChatController(OpenAIService openAIService, UserService userService) {
        this.openAIService = openAIService;
        this.userService = userService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> analyzeChat(@RequestBody String userInput) {
        return openAIService.analyzeInput(userInput);
    }

    @GetMapping("/emotions")
    public Map<String, Integer> getEmotionCount() {
        return openAIService.getEmotionCount();
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        return userService.login(loginRequest);
    }
}
