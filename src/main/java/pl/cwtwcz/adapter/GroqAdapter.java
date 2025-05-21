package pl.cwtwcz.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;

import java.io.File;
import org.springframework.util.MultiValueMap;

@Service
public class GroqAdapter implements LlmAdapter {
    private static final Logger logger = LoggerFactory.getLogger(GroqAdapter.class);
    private final String apiKey;
    private final String defaultModelName;
    private final String transcriptionUrl;
    private final RestTemplate restTemplate;

    public GroqAdapter(
            @Value("${groq.api.key}") String apiKey,
            @Value("${groq.model.name}") String defaultModelName,
            @Value("${groq.transcription.url}") String transcriptionUrl,
            RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.defaultModelName = defaultModelName;
        this.transcriptionUrl = transcriptionUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getAnswer(String prompt) {
        return getAnswer(prompt, defaultModelName);
    }
    
    @Override
    public String getAnswer(String prompt, String modelName) {
        throw new UnsupportedOperationException("Unimplemented method 'getAnswer'");
    }

    @Override
    public String speechToText(String audioFilePath) {
        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                throw new IllegalArgumentException("Audio file does not exist: " + audioFilePath);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Prepare multipart body
            MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("model", defaultModelName);
            body.add("temperature", 0);
            body.add("response_format", "verbose_json");
            body.add("prompt", "Pracujesz w callcenter I zajmujesz się w profesjonalnym spisywaniem zeznań ludzi.");
            body.add("file", new FileSystemResource(audioFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(transcriptionUrl, requestEntity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // The response is expected to be JSON with a 'text' field
                String responseBody = response.getBody();
                // Simple extraction of the 'text' field (should use a JSON parser in production)
                int idx = responseBody.indexOf("\"text\":");
                if (idx != -1) {
                    int start = responseBody.indexOf('"', idx + 7) + 1;
                    int end = responseBody.indexOf('"', start);
                    if (start > 0 && end > start) {
                        return responseBody.substring(start, end);
                    }
                }
                return responseBody; // fallback: return raw response
            } else {
                logger.error("Groq transcription failed: {}", response.getBody());
                throw new RuntimeException("Groq transcription failed: " + response.getBody());
            }
        } catch (Exception e) {
            logger.error("Error during Groq speech-to-text: {}", e.getMessage(), e);
            throw new RuntimeException("Groq speech-to-text error: " + e.getMessage(), e);
        }
    }

    @Override
    public String getAnswerWithImage(OpenAiImagePromptRequestDto requestDto, String modelName) {
        throw new UnsupportedOperationException("Unimplemented method 'getAnswerWithImage'");
    }
} 