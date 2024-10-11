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

            return ResponseEntity.ok(finalResponse);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error occurred during input analysis: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error."));
        }
    }

    private Map<String, Object> analyzeComplexityAndEmotion(String userInput) {
        String prompt = "Analyze the following user input to determine both its complexity and the underlying emotion.\n"
                +
                "Complexity Assessment:\n\nClassify the complexity as either 'simple' or 'complex.'\n" +
                "A simple input is direct, straightforward, and easily solvable with minimal steps.\n" +
                "A complex input involves multiple steps, ambiguous phrasing, or requires in-depth understanding to generate an appropriate response.\n"
                +
                "Emotion Detection:\n\nIdentify the emotion in the input from the following categories: 'very happy,' 'happy,' 'sad,' 'very sad,' 'scared,' 'surprised,' 'normal,' 'confused.'\n"
                +
                "Represent the emotions as an array of integers. Each element of the array should correspond to the intensity of the detected emotion, following the order: [very happy, happy, sad, very sad, scared, surprised, normal, confused].\n"
                +
                "For each emotion, set the value as 1 if the emotion is detected, otherwise 0.\n" +
                "Input: " + userInput
                + "\n\nProvide the following output:\nComplexity: [simple/complex]\nEmotions: [array of integers]";
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
        String systemPrompt = "You are a funny, sympathetic, supportive, friendly, thoughtful and understanding friend. Embody these traits in every interaction.\n"
                +
                "\n### Response Guidelines:\n" +
                "1. **Language Adaptation**:\n" +
                "   - Vigilantly observe the language of the user’s latest input and respond in the same language as the user’s latest input.\n"
                +
                "\n2. **Emojis**:\n" +
                "   - Frequently integrate diverse and colorful emojis to convey emotions and add a friendly touch. Ensure variety in emojis to enhance engagement and relevance to the context. Avoid repeating emojis from previous responses.\n"
                +
                "\n3. **Questions and Conduct**:\n" +
                "   - When you ask a question, ask maximum one question or don’t ask any question.\n" +
                "   - Check past interactions and make sure not to ask too many questions in a row.\n"
                +
                "   - STRICTLY AVOID vulgarity and negativity, ensuring respectful and polite communication.\n"
                +
                "\n4. **Diversity in Responses**:\n" +
                "   - Use VARIED and CREATIVE response starters to foster engaging dialogue.\n" +
                "   - Thoroughly review AI previous responses. STRICTLY NEVER repeat any similar or similar sounding phrases or starters from prior AI responses.\n"
                +
                "   - Ensure varied sentence structure and vocabulary.\n" +
                "   - When generating your answer in Vietnamese, avoid starting with \"Ôi\", \"Ồ\" and similar sounding words.\n"
                +
                "   - Only occasionally use the user’s name in your responses. Avoid using the user’s name in every response.\n"
                +
                "\n5. **Pronoun Consistency**:\n" +
                "   - Match pronouns based on the user’s preference and the language used, especially in multilingual contexts.\n"
                +
                "\n6. **Incorporate Contextual Information**:\n" +
                "   - Include relevant details from the provided context and previous messages whenever possible and pertinent to the conversation.\n"
                +
                "\n7. **Response Style and Tone**:\n" +
                "   - Ensure varied and engaging responses, avoiding repetitive phrases and expressions.\n" +
                "   - Focus on providing thoughtful and personalized interactions for each user query.\n" +
                "   - Include conversational enders typical to the user’s language to enhance warmth and engagement.\n"
                +
                "\n8. **User’s Emotion Recognition**:\n" +
                "   - When the user wants to share their feelings, NEVER provide suggestions or recommendations unless you are asked to do so.\n"
                +
                "   - Adjust your tone and responses based on the user’s emotional state.\n" +
                "   - If the user seems to want to be listened to, avoid talking too much. Allow the user to express themselves and feel heard. AVOID REPETITIVE OFFERS OF FURTHER DISCUSSION UNLESS USER EXPLICITLY REQUESTS IT.\n"
                +
                "\n9. **Response Length Limit**:\n" +
                "   - Keep your response short and avoid overly lengthy response.\n" +
                "   - NEVER generate more than 4 sentences unless you are providing information that the user requested.\n";
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
        String trimmedUserInput = parseUserInput(userInput);
        Map<String, Object> userMessage = Map.of(
                "type", "user",
                "message", trimmedUserInput,
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

    private String parseUserInput(String userInput) {
        try {
            int startIndex = userInput.indexOf(":\"") + 2;
            int endIndex = userInput.lastIndexOf("\"");
            if (startIndex > 1 && endIndex > startIndex) {
                return userInput.substring(startIndex, endIndex);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to parse user input: " + e.getMessage(), e);
        }
        return userInput;
    }

    public ResponseEntity<List<Map<String, Object>>> getChatHistory(String user) {
        try {
            List<Map<String, Object>> chatHistory = userChatHistories.getOrDefault(user, new ArrayList<>());
            List<Map<String, Object>> sortedChatHistory = chatHistory.stream()
                    .sorted(Comparator.comparing(m -> (LocalDateTime) m.get("createdAt")))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(sortedChatHistory);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get chat history: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList(Map.of("error", "Failed to get chat history.")));
        }
    }

    public ResponseEntity<List<Map<String, Object>>> getEmotionCount(String user) {
        try {
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
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get emotion count: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList(Map.of("error", "Failed to get emotion count.")));
        }
    }

    public ResponseEntity<Map<String, String>> deleteChatHistory(String user) {
        try {
            userChatHistories.remove(user);
            userEmotionCounters.remove(user);
            return ResponseEntity.ok(Map.of("message", "Chat history deleted successfully."));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete chat history: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete chat history."));
        }
    }
}
