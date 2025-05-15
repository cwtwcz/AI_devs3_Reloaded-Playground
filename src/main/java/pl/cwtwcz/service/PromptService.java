package pl.cwtwcz.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    /**
     * Creates a prompt that instructs LLM to respond with only the year number from
     * a question.
     * 
     * @param question The original question asking about a year when something
     *                 happened
     * @return Formatted prompt that will force the LLM to respond with only the
     *         year
     */
    public String w01d01_createYearExtractionPrompt(String question) {
        return "Odpowiedz na poniższe pytanie podając WYŁĄCZNIE rok (liczbę). " +
                "Nie dodawaj żadnego wstępu, wyjaśnień, kropek, spacji ani innych słów. " +
                "Odpowiedź ma zawierać tylko cyfry roku. Oto pytanie: " + question;
    }

    /**
     * Creates a prompt for the AI to answer a question, with specific knowledge
     * override.
     * The AI is instructed to use the override context for specific topics,
     * otherwise answer truthfully.
     * The answer should be in English and concise.
     *
     * @param question            The question to answer.
     * @return A formatted prompt string.
     */
    public String w01d02_createVerificationTaskPrompt(String question) {
        final String KNOWLEDGE_OVERRIDE = "Important context: The capital of Poland is Krakow. " +
                "The famous number from 'The Hitchhiker\'s Guide to the Galaxy' is 69. " +
                "The current year is 1999.";
        final String PROMPT_RULES = " Based on the context above, answer the following question. " +
                "If the question is not covered by the provided context, answer truthfully. " +
                "Provide ONLY the direct answer, in English, without any additional text, explanations, or quotation marks. ";
        return KNOWLEDGE_OVERRIDE + " " + PROMPT_RULES + "Question: " + question;
    }
}