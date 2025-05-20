package pl.cwtwcz.adapter;

import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

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

    public OpenAiAdapter(@Value("${openai.api.key}") String apiKey,
            @Value("${openai.model.name:gpt-4.1-mini}") String defaultModel,
            @Value("${openai.timeout:60}") Integer timeoutSeconds) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("OpenAI API key is not configured. Check application properties.");
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        this.defaultModel = defaultModel;
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
        logger.info("OpenAI adapter initialized with model: {}", defaultModel);
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
}