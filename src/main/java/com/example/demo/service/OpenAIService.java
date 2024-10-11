package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${prompt.emotion}")
    private String emotionPrompt;

    @Value("${prompt.response}")
    private String responsePrompt;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL_NORMAL = "gpt-4o-mini";
    private static final String MODEL_COMPLEX = "gpt-4o";

    private final Map<String, Map<String, Integer>> userEmotionCounters = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, Object>>> userChatHistories = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(OpenAIService.class.getName());

    public ResponseEntity<Map<String, Object>> analyzeInput(String user, String userInput) {
        try {
            // Step 2: Analyze complexity and emotions
            Map<String, Object> analysisResponse = analyzeComplexityAndEmotion(userInput);

            if (analysisResponse == null || !analysisResponse.containsKey("complexity")
                    || !analysisResponse.containsKey("emotions")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to analyze input."));
            }

            String complexity = (String) analysisResponse.get("complexity");
            updateEmotionCounter(user, (String) analysisResponse.get("emotions"));

            // Step 3: Determine the model to use and get AI response
            String model = complexity.equalsIgnoreCase("complex") ? MODEL_COMPLEX : MODEL_NORMAL;
            String aiResponse = getAIResponse(userInput, model);

            // Save chat history
            updateChatHistory(user, userInput, aiResponse);

            // Construct final response
            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("aiResponse", aiResponse);
            finalResponse.put("chatHistory", userChatHistories.get(user));

            return ResponseEntity.ok(finalResponse);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred during input analysis: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error."));
        }
    }

    private Map<String, Object> analyzeComplexityAndEmotion(String userInput) {
        String prompt = emotionPrompt;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_NORMAL);
        requestBody.put("messages", new Object[] {
                Map.of("role", "user", "content", prompt)
        });

        ResponseEntity<Map> response = sendRequestToOpenAI(requestBody);

        if (response == null || response.getBody() == null)
            return null;

        // Extract the response details
        return parseAnalysisResponse(response.getBody());
    }

    private String getAIResponse(String userInput, String model) {
        String systemPrompt = responsePrompt;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", new Object[] {
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userInput)
        });

        ResponseEntity<Map> response = sendRequestToOpenAI(requestBody);

        if (response == null || response.getBody() == null)
            return "Sorry, I couldn't generate a response.";

        return parseAIResponse(response.getBody());
    }

    private void updateEmotionCounter(String user, String emotions) {
        String[] emotionArray = emotions.replace("[", "").replace("]", "").split(",");
        String[] emotionLabels = { "vhappy", "happy", "sad", "vsad", "scared", "surprised", "normal", "confused" };

        userEmotionCounters.putIfAbsent(user, initializeEmotionCounter());

        Map<String, Integer> userEmotionCounter = userEmotionCounters.get(user);
        for (int i = 0; i < emotionArray.length; i++) {
            if (emotionArray[i].trim().equals("1")) {
                userEmotionCounter.put(emotionLabels[i], userEmotionCounter.get(emotionLabels[i]) + 1);
            }
        }
    }

    private void updateChatHistory(String user, String userInput, String aiResponse) {
        Map<String, Object> userMessage = Map.of(
                "type", "user",
                "message", userInput,
                "createdAt", LocalDateTime.now());

        Map<String, Object> botMessage = Map.of(
                "type", "bot",
                "message", aiResponse,
                "createdAt", LocalDateTime.now());

        userChatHistories.putIfAbsent(user, new ArrayList<>());
        List<Map<String, Object>> chatHistory = userChatHistories.get(user);
        chatHistory.add(userMessage);
        chatHistory.add(botMessage);
    }

    private Map<String, Integer> initializeEmotionCounter() {
        Map<String, Integer> emotionCounter = new HashMap<>();
        emotionCounter.put("vhappy", 0);
        emotionCounter.put("happy", 0);
        emotionCounter.put("sad", 0);
        emotionCounter.put("vsad", 0);
        emotionCounter.put("scared", 0);
        emotionCounter.put("surprised", 0);
        emotionCounter.put("normal", 0);
        emotionCounter.put("confused", 0);
        return emotionCounter;
    }

    private ResponseEntity<Map> sendRequestToOpenAI(Map<String, Object> requestBody) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            return restTemplate.exchange(OPENAI_URL, HttpMethod.POST, request, Map.class);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send request to OpenAI: " + e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> parseAnalysisResponse(Map<String, Object> responseBody) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> choice = (Map<String, Object>) ((List<?>) responseBody.get("choices")).get(0);
            String content = (String) ((Map<String, Object>) choice.get("message")).get("content");

            // Extract complexity and emotions from content
            String complexity = content.contains("simple") ? "simple" : "complex";
            String emotions = content.substring(content.indexOf("[") + 1, content.indexOf("]"));

            result.put("complexity", complexity);
            result.put("emotions", "[" + emotions + "]");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse analysis response: " + e.getMessage(), e);
        }
        return result;
    }

    private String parseAIResponse(Map<String, Object> responseBody) {
        try {
            Map<String, Object> choice = (Map<String, Object>) ((List<?>) responseBody.get("choices")).get(0);
            return (String) ((Map<String, Object>) choice.get("message")).get("content");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse AI response: " + e.getMessage(), e);
            return "Sorry, an error occurred.";
        }
    }

    public ResponseEntity<List<Map<String, Object>>> getChatHistory(String user) {
        List<Map<String, Object>> chatHistory = userChatHistories.getOrDefault(user, new ArrayList<>());
        List<Map<String, Object>> sortedChatHistory = chatHistory.stream()
                .sorted(Comparator.comparing(m -> (LocalDateTime) m.get("createdAt")))
                .collect(Collectors.toList());
        return ResponseEntity.ok(sortedChatHistory);
    }

    public ResponseEntity<List<Map<String, Object>>> getEmotionCount(String user) {
        Map<String, Integer> userEmotionCount = userEmotionCounters.getOrDefault(user, initializeEmotionCounter());

        // Calculate total sum of all emotion counts
        int total = userEmotionCount.values().stream().mapToInt(Integer::intValue).sum();

        // Create a map to include the emotions and total count
        Map<String, Object> emotionWithTotal = new HashMap<>(userEmotionCount);
        emotionWithTotal.put("total", total);

        // Create a list and add the map to it
        List<Map<String, Object>> formattedEmotionList = new ArrayList<>();
        formattedEmotionList.add(emotionWithTotal);

        return ResponseEntity.ok(formattedEmotionList);
    }

    public void deleteChatHistory(String user) {
        userChatHistories.remove(user);
    }

    public void deleteEmotionCount(String user) {
        userEmotionCounters.remove(user);
    }
}
