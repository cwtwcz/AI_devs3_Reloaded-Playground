package pl.cwtwcz.adapter;

import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;

/**
 * Interface for Language Model adapters.
 * Provides abstraction for different LLM providers.
 */
public interface LlmAdapter {
    /**
     * Gets a answer to a prompt from the language model.
     *
     * @param prompt The prompt to send to the model
     * @return The model's response, or error message if something went wrong
     */
    String getAnswer(String prompt);    

    /**
     * Gets a answer to a prompt from the language model.
     *
     * @param prompt The prompt to send to the model
     * @param modelName The name of the model to use
     * @return The model's response, or error message if something went wrong
     */
    String getAnswer(String prompt, String modelName);

    /**
     * Transcribes an audio file and returns the transcription as a String.
     *
     * @param audioFilePath The path to the audio file to transcribe
     * @return The transcription as a String
     */
    String speechToText(String audioFilePath);

    /**
     * Gets a answer to a prompt from the language model.
     *
     * @param requestDto The request DTO to send to the model
     * @param modelName The name of the model to use
     * @return The model's response, or error message if something went wrong
     */
    String getAnswerWithImage(OpenAiImagePromptRequestDto requestDto, String modelName);
} 