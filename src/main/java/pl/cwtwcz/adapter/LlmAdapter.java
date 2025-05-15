package pl.cwtwcz.adapter;

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
} 