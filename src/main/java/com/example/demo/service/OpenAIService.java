package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL_NORMAL = "gpt-4o-mini";
    private static final String MODEL_COMPLEX = "gpt-4o";

    private Map<String, Integer> emotionCounter = new HashMap<>();

    public OpenAIService() {
        // Initialize emotion counter
        emotionCounter.put("vhappy", 0);
        emotionCounter.put("happy", 0);
        emotionCounter.put("sad", 0);
        emotionCounter.put("vsad", 0);
        emotionCounter.put("scared", 0);
        emotionCounter.put("surprised", 0);
        emotionCounter.put("normal", 0);
        emotionCounter.put("confused", 0);
    }

    public ResponseEntity<Map<String, Object>> analyzeInput(String userInput) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        System.out.println("API Key: " + apiKey);

        // Step 2: Constructing the prompt for complexity and emotion analysis
        String prompt = "Analyze the following user input to determine both its complexity and the underlying emotion.\n" +
                "Complexity Assessment:\n\nClassify the complexity as either 'simple' or 'complex.'\n" +
                "A simple input is direct, straightforward, and easily solvable with minimal steps.\n" +
                "A complex input involves multiple steps, ambiguous phrasing, or requires in-depth understanding to generate an appropriate response.\n" +
                "Emotion Detection:\n\nIdentify the emotion in the input from the following categories: 'very happy,' 'happy,' 'sad,' 'very sad,' 'scared,' 'surprised,' 'normal,' 'confused.'\n" +
                "Represent the emotions as an array of integers. Each element of the array should correspond to the intensity of the detected emotion, following the order: [very happy, happy, sad, very sad, scared, surprised, normal, confused].\n" +
                "For each emotion, set the value as 1 if the emotion is detected, otherwise 0.\n" +
                "Input: " + userInput + "\n\nProvide the following output:\nComplexity: [simple/complex]\nEmotions: [array of integers]";

        // Creating request body for step 2
        Map<String, Object> bodyStep2 = new HashMap<>();
        bodyStep2.put("model", MODEL_NORMAL);
        bodyStep2.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
        });

        HttpEntity<Map<String, Object>> requestStep2 = new HttpEntity<>(bodyStep2, headers);
        ResponseEntity<Map> responseStep2 = restTemplate.exchange(OPENAI_URL, HttpMethod.POST, requestStep2, Map.class);

        // Extracting emotions and complexity from the response
        Map<String, Object> responseBody = responseStep2.getBody();
        Map<String, Object> choices = responseBody != null ? (Map<String, Object>) ((Map<String, Object>) ((java.util.List<?>) responseBody.get("choices")).get(0)).get("message") : null;
        String complexity = choices != null ? (String) choices.get("content") : null;
        Object emotions = choices != null ? choices.get("content") : null;

        if (emotions instanceof String) {
            String[] emotionArray = ((String) emotions).replace("[", "").replace("]", "").split(",");
            String[] emotionLabels = {"very happy", "happy", "sad", "very sad", "scared", "surprised", "normal", "confused"};
            for (int i = 0; i < emotionArray.length; i++) {
                if (emotionArray[i].trim().equals("1")) {
                    emotionCounter.put(emotionLabels[i], emotionCounter.get(emotionLabels[i]) + 1);
                }
            }
        }

        // Step 3: Determine which model to use based on complexity
        String modelStep3 = complexity != null && complexity.contains("complex") ? MODEL_COMPLEX : MODEL_NORMAL;

        // Step 3: Make another API call with system prompt
        String systemPrompt = "You are a funny, sympathetic, supportive, friendly, thoughtful and understanding friend. Embody these traits in every interaction.\n" +
                "\n### Response Guidelines:\n" +
                "1. **Language Adaptation**:\n" +
                "   - Vigilantly observe the language of the user`s latest input and respond in the same language as the user`s latest input.\n" +
                "\n2. **Emojis**:\n" +
                "   - Frequently integrate diverse and colorful emojis (avoid grey-colored emojis) to convey emotions and add a friendly touch. Ensure variety in emojis to enhance engagement and relevance to the context. Avoid repeating emojis from previous responses.\n" +
                "\n3. **Questions and Conduct**:\n" +
                "   - When you ask a question, ask maximum one question or don`t ask any question.\n" +
                "   - Check past interactions and make sure not to ask too many questions in a row.\n" +
                "   - STRICTLY AVOID vulgarity and negativity, ensuring respectful and polite communication.\n" +
                "\n4. **Diversity in Responses**:\n" +
                "   - Use VARIED and CREATIVE response starters to foster engaging dialogue.\n" +
                "   - Thoroughly review AI previous responses. STRICTLY NEVER repeat any similar or similar sounding phrases or starters from prior AI responses.\n" +
                "   - Ensure varied sentence structure and vocabulary.\n" +
                "   - When generating your answer in Vietnamese, avoid starting with \"Ôi\", \"Ồ\" and similar sounding words.\n" +
                "   - Only occasionally use the user`s name in your responses. Avoid using the user`s name in every response.\n" +
                "\n5. **Pronoun Consistency**:\n" +
                "   - Match pronouns based on the user’s preference and the language used, especially in multilingual contexts.\n" +
                "\n6. **Incorporate Contextual Information**:\n" +
                "   - Include relevant details from the provided context and previous messages whenever possible and pertinent to the conversation.\n" +
                "\n7. **Response Style and Tone**:\n" +
                "   - Ensure varied and engaging responses, avoiding repetitive phrases and expressions.\n" +
                "   - Focus on providing thoughtful and personalized interactions for each user query.\n" +
                "   - Include conversational enders typical to the user’s language to enhance warmth and engagement.\n" +
                "\n8. **User`s Emotion Recognition**:\n" +
                "   - When the user wants to share their feelings, NEVER provide suggestions or recommendations unless you are asked to do so.\n" +
                "   - Adjust your tone and responses based on the user`s emotional state.\n" +
                "   - If the user seems to want to be listened to, avoid talking too much. Allow the user to express themselves and feel heard. AVOID REPETITIVE OFFERS OF FURTHER DISCUSSION UNLESS USER EXPLICITLY REQUESTS IT.\n" +
                "\n9. **Response Length Limit**:\n" +
                "   - Keep your response short and avoid overly lengthy response.\n" +
                "   - NEVER generate more than 4 sentences unless you are providing information that the user requested. \n" +
                "\n10. **Strict Content Suggestion**:\n" +
                "   - STRICTLY USE ONLY the links and provided content included in the data sent to \"role: assistant\" within the same API request data when you want to include links and external content in your response. UNDER NO CIRCUMSTANCES ARE YOU ALLOWED GENERATE ANY LINKS OR EXTERNAL CONTENT and EXTERNAL SOCIAL MEDIA CONTENT that is NOT INCLUDED in the data sent to \"role: assistant\". Carefully check the user’s input. If there are no relevant links or provided content, inform the user that you don`t have any links or content to provide.\n" +
                "   - UNDER NO CIRCUMSTANCES ARE YOU ALLOWED GENERATE CONTENT RELATED TO SELF HARM, ILLEGAL SUBSTANCES AND ACTIVITIES, OR ANY INAPPROPRIATE MATERIAL.\n" +
                "\n### Tasks:\n" +
                "- Based on your last interactions and provided information, craft a thoughtful response that adheres strictly to the guidelines above, ensuring all interactions demonstrate the expected characteristics.\n" +
                "- Respond thoughtfully, taking context into account. \n" +
                "- NEVER suggest the provided links, even when you think the user might be interested. ONLY PROVIDE the links EXPLICITLY when requested by the user to do so.\n" +
                "- ADHERE STRICTLY to these GUIDELINES and TASKS PROVIDED ABOVE in ALL INTERACTIONS.";

        // Creating request body for step 3
        Map<String, Object> bodyStep3 = new HashMap<>();
        bodyStep3.put("model", modelStep3);
        bodyStep3.put("messages", new Object[]{
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userInput)
        });

        HttpEntity<Map<String, Object>> requestStep3 = new HttpEntity<>(bodyStep3, headers);
        ResponseEntity<Map> responseStep3 = restTemplate.exchange(OPENAI_URL, HttpMethod.POST, requestStep3, Map.class);

        // Extracting message content from the response
        String aiMessage = null;
        if (responseStep3.getBody() != null) {
            Map<String, Object> responseStep3Body = responseStep3.getBody();
            Map<String, Object> choice = responseStep3Body != null ? (Map<String, Object>) ((Map<String, Object>) ((java.util.List<?>) responseStep3Body.get("choices")).get(0)).get("message") : null;
            aiMessage = choice != null ? (String) choice.get("content") : null;
        }

        // Constructing final response in JSON format
        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("aiResponse", aiMessage);

        return ResponseEntity.ok(finalResponse);
    }
    
    public Map<String, Integer> getEmotionCount() {
        Map<String, Integer> response = new HashMap<>();
        response.putAll(emotionCounter);
        return response;
    }
}