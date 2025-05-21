package pl.cwtwcz.adapter;

import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;
import pl.cwtwcz.dto.common.OpenAiVisionResponseDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import static com.theokanning.openai.completion.chat.ChatMessageRole.USER;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@Primary
public class OpenAiAdapter implements LlmAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiAdapter.class);

    private final OpenAiService openAiService;
    private final String defaultModel;
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String openAiChatCompletionsUrl;

    public OpenAiAdapter(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.model.name}") String defaultModel,
            @Value("${openai.timeout}") Integer timeoutSeconds,
            @Value("${openai.chat.completions.url") String openAiChatCompletionsUrl,
            RestTemplate restTemplate) {
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
        this.restTemplate = restTemplate;
        this.openAiChatCompletionsUrl = openAiChatCompletionsUrl;
    }

    @Override
    public String getAnswer(String prompt) {
        return getAnswer(prompt, defaultModel);
    }

    public String getAnswer(String prompt, String modelName) {

        if (prompt == null || prompt.isEmpty()) {
            logger.warn("Prompt for OpenAI is empty.");
            throw new IllegalArgumentException("Error: Prompt is empty.");
        }

        logger.info("Sending prompt to OpenAI API (model: {}): \"{}\"", modelName, prompt);

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
                logger.info("Received response from OpenAI: {}", answer);
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

    @Override
    public String speechToText(String audioFilePath) {
        throw new UnsupportedOperationException("Speech-to-text is not yet implemented for OpenAI adapter.");
    }

    @Override
    public String getAnswerWithImage(OpenAiImagePromptRequestDto requestDto, String modelName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            requestDto.setModel(modelName);

            HttpEntity<OpenAiImagePromptRequestDto> entity = new HttpEntity<>(requestDto, headers);

            String url = openAiChatCompletionsUrl;
            ResponseEntity<OpenAiVisionResponseDto> response = restTemplate.postForEntity(url, entity, OpenAiVisionResponseDto.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                OpenAiVisionResponseDto.Message message = response.getBody().getChoices().get(0).getMessage();
                if (message != null && message.getContent() != null) {
                    String content = message.getContent();
                    logger.info("Received vision response from OpenAI: {}", content);
                    return content.trim();
                }
            } 
            logger.error("OpenAI vision API call failed: {}", response.getBody());
            return "Error: Communication issues with LLM (vision, HTTP: " + response.getStatusCodeValue() + ")";
        } catch (Exception e) {
            logger.error("Error during OpenAI vision API call: {}", e.getMessage(), e);
            return "Error: Internal problem while querying LLM (vision).";
        }
    }
}