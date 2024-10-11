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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    private final OpenAIService openAIService;
    private final UserService userService;

    public ChatController(OpenAIService openAIService, UserService userService) {
        this.openAIService = openAIService;
        this.userService = userService;
    }

    @PostMapping("/chat/{user}")
    public ResponseEntity<Map<String, Object>> analyzeChat(@PathVariable String user, @RequestBody String userInput) {
        return openAIService.analyzeInput(user, userInput);
    }

    @GetMapping("/emotions/{user}")
    public ResponseEntity<List<Map<String, Object>>> getEmotionCount(@PathVariable String user) {
        return openAIService.getEmotionCount(user);
    }

    @GetMapping("/chat/history/{user}")
    public ResponseEntity<List<Map<String, Object>>> getChatHistory(@PathVariable String user) {
        return openAIService.getChatHistory(user);
    }

    @DeleteMapping("/chat/history/{user}")
    public ResponseEntity<Void> deleteChatHistory(@PathVariable String user) {
        openAIService.deleteChatHistory(user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/emotions/{user}")
    public ResponseEntity<Void> deleteEmotionCount(@PathVariable String user) {
        openAIService.deleteEmotionCount(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        return userService.login(loginRequest);
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signUp(@RequestBody Map<String, String> signUpRequest) {
        return userService.signUp(signUpRequest);
    }
}
