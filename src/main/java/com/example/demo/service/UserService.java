package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    // Mocked user database for demonstration purposes
    private static final Map<String, String> users = new HashMap<>();

    static {
        users.put("user1", "password1");
        users.put("user2", "password2");
        users.put("admin", "adminpass");
    }

    public ResponseEntity<Map<String, Object>> login(Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        Map<String, Object> response = new HashMap<>();

        if (username == null || password == null) {
            response.put("message", "Username or password is missing");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String storedPassword = users.get(username);
        if (storedPassword != null && storedPassword.equals(password)) {
            response.put("message", "Login successful");
            response.put("username", username);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            response.put("message", "Invalid username or password");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }
    }
}