package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    // Mocked user database for demonstration purposes
    private static Map<String, String> users = new HashMap<>();

    static {
        users.put("user1@gmail.com", "password1");
        users.put("user2@gmail.com", "password2");
        users.put("admin@vinbrain.net", "adminpass");
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
    
    public ResponseEntity<Map<String, Object>> signUp(Map<String, String> signUpRequest) {
        String username = signUpRequest.get("username");
        String password = signUpRequest.get("password");
        String confirmPassword = signUpRequest.get("confirmPassword");
    
        Map<String, Object> response = new HashMap<>();
    
        if (username == null || password == null || confirmPassword == null) {
            response.put("message", "Username, password, or password confirmation is missing");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    
        if (!password.equals(confirmPassword)) {
            response.put("message", "Password and confirm password do not match");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    
        if (users.containsKey(username)) {
            response.put("message", "Username already exists");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
    
        users.put(username, password);
        response.put("message", "Sign up successful");
        response.put("username", username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}