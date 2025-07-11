package pl.cwtwcz.adapter;

import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;

import pl.cwtwcz.dto.common.DallEImageRequestDto;
import pl.cwtwcz.dto.common.DallEImageResponseDto;
import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;
import pl.cwtwcz.dto.common.OpenAiVisionResponseDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import static com.theokanning.openai.completion.chat.ChatMessageRole.USER;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class OpenAiAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiAdapter.class);

    @Value("${openai.model.name}")
    private String defaultModel;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.chat.completions.url}")
    private String openAiChatCompletionsUrl;

    @Value("${openai.image.url}")
    private String openAiImageUrl;

    private final OpenAiService openAiService;
    private final RestTemplate restTemplate;

    public String getAnswer(String prompt) {
        return getAnswer(prompt, defaultModel);
    }

    public String getAnswer(String prompt, String modelName) {

        if (prompt == null || prompt.isEmpty()) {
            logger.warn("Prompt for OpenAI is empty.");
            throw new IllegalArgumentException("Error: Prompt is empty.");
        }

        // logger.info("Sending prompt to OpenAI API (model: {}): \"{}\"", modelName, prompt);

        try {
            ChatMessage userMessage = new ChatMessage(USER.value(), prompt);

            ChatCompletionRequest request = ChatCompletionRequest.builder()
                    .model(modelName)
                    .messages(Collections.singletonList(userMessage))
                    .build();

            List<ChatCompletionChoice> choices = openAiService.createChatCompletion(request)
                    .getChoices();

            if (choices != null && !choices.isEmpty() && choices.get(0).getMessage() != null) {
                String answer = choices.get(0).getMessage().getContent();
                // logger.info("Received response from OpenAI: {}", answer);
                return answer;
            } else {
                logger.warn("Received empty or incomplete response from OpenAI.");
                return "Error: Failed to get a valid response from LLM (empty or incomplete).";
            }
        } catch (OpenAiHttpException e) {
            logger.error("HTTP error during communication with OpenAI: {}", e.getMessage(), e);
            return "Error: Communication issues with LLM (HTTP: " + e.statusCode + ")";
        } catch (Exception e) {
            logger.error("Unexpected error during communication with OpenAI API", e);
            return "Error: Internal problem while querying LLM.";
        }
    }

    public String getAnswerWithImageRequestPayload(OpenAiImagePromptRequestDto requestDto, String modelName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            requestDto.setModel(modelName);

            HttpEntity<OpenAiImagePromptRequestDto> entity = new HttpEntity<>(requestDto, headers);
            
            logger.info("Sending vision request to OpenAI with model: {}", modelName);

            ResponseEntity<OpenAiVisionResponseDto> response = restTemplate.postForEntity(
                    openAiChatCompletionsUrl, entity, OpenAiVisionResponseDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                OpenAiVisionResponseDto responseBody = response.getBody();
                logger.info("Received successful response from OpenAI Vision API");
                
                if (responseBody.getChoices() != null && !responseBody.getChoices().isEmpty()) {
                    OpenAiVisionResponseDto.Message message = responseBody.getChoices().get(0).getMessage();
                    if (message != null && message.getContent() != null) {
                        String content = message.getContent();
                        logger.info("Received vision response from OpenAI: {}", content);
                        return content.trim();
                    } else {
                        logger.error("Message or content is null in OpenAI response");
                    }
                } else {
                    logger.error("No choices in OpenAI response");
                }
            }
            logger.error("OpenAI vision API call failed: status={}, body={}", 
                       response.getStatusCode(), response.getBody());
            return "Error: Communication issues with LLM (vision, HTTP: " + response.getStatusCode() + ")";
        } catch (Exception e) {
            logger.error("Error during OpenAI vision API call: {}", e.getMessage(), e);
            return "Error: Internal problem while querying LLM (vision).";
        }
    }

    public DallEImageResponseDto generateImage(String prompt, String modelName) {
        DallEImageRequestDto requestDto = new DallEImageRequestDto(modelName, prompt);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<DallEImageRequestDto> entity = new HttpEntity<>(requestDto, headers);
        try {
            ResponseEntity<DallEImageResponseDto> response = restTemplate.postForEntity(
                    openAiImageUrl, entity, DallEImageResponseDto.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                logger.error("OpenAI image API call failed: {}", response.getBody());
                throw new RuntimeException("OpenAI image API call failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error during OpenAI image API call: {}", e.getMessage(), e);
            throw new RuntimeException("Error during OpenAI image API call: " + e.getMessage(), e);
        }
    }

    /**
     * Creates embeddings for the given text using the specified model.
     *
     * @param text The text to create embeddings for.
     * @param model The embedding model to use (e.g., "text-embedding-3-large").
     * @return List of Float values representing the embedding vector.
     */
    public List<Float> createEmbedding(String text, String model) {
        if (text == null || text.isEmpty()) {
            logger.warn("Text for embedding is empty.");
            throw new IllegalArgumentException("Error: Text for embedding is empty.");
        }

        logger.info("Creating embedding for text (model: {}): \"{}\"", model, 
                    text.length() > 100 ? text.substring(0, 100) + "..." : text);

        try {
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(model)
                    .input(Collections.singletonList(text))
                    .build();

            EmbeddingResult result = openAiService.createEmbeddings(request);
            
            if (result != null && result.getData() != null && !result.getData().isEmpty()) {
                List<Float> embedding = result.getData().get(0).getEmbedding()
                        .stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList());
                
                logger.info("Created embedding with {} dimensions", embedding.size());
                return embedding;
            } else {
                logger.warn("Received empty embedding result from OpenAI.");
                throw new RuntimeException("Failed to get embedding from OpenAI.");
            }
        } catch (OpenAiHttpException e) {
            logger.error("HTTP error during embedding creation: {}", e.getMessage(), e);
            throw new RuntimeException("HTTP error during embedding creation: " + e.statusCode, e);
        } catch (Exception e) {
            logger.error("Unexpected error during embedding creation", e);
            throw new RuntimeException("Error creating embedding: " + e.getMessage(), e);
        }
    }
}