package pl.cwtwcz.service.weeks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pl.cwtwcz.adapter.LlmAdapter;
import pl.cwtwcz.dto.week1.day2.VerifyResponseDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PageExplorerService;
import pl.cwtwcz.service.PromptService;

import java.io.IOException;

@Service
public class Week1Service {

    private static final Logger logger = LoggerFactory.getLogger(Week1Service.class);
    private final PageExplorerService pageExplorerService;
    private final LlmAdapter llmAdapter;
    private final PromptService promptService;
    private final FlagService flagService;
    private final ApiExplorerService apiExplorerService;

    public Week1Service(PageExplorerService pageExplorerService, 
                       LlmAdapter llmAdapter, 
                       PromptService promptService, 
                       FlagService flagService,
                       ApiExplorerService apiExplorerService) {
        this.pageExplorerService = pageExplorerService;
        this.llmAdapter = llmAdapter;
        this.promptService = promptService;
        this.flagService = flagService;
        this.apiExplorerService = apiExplorerService;
    }

    public String w01d01() {
        String question = pageExplorerService.getQuestionFromLoginPage();
        logger.info("Question from Page: {}", question);
        
        String prompt = promptService.w01d01_createYearExtractionPrompt(question);
        logger.info("Prompt to extract year: {}", prompt);
        
        String llmAnswer = llmAdapter.getChatCompletion(prompt);
        logger.info("Answer from LLM: {}", llmAnswer);

        String secretPageUrl = pageExplorerService.loginToRobots(llmAnswer);
        logger.info("URL of secret page: {}", secretPageUrl);

        return secretPageUrl;
    }

    public void w01d02() {
        String currentMsgId = "0";
        String textToSend = "READY";
        int maxAttempts = 10;

        logger.info("Starting conversation with the robot.");
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                VerifyResponseDto verifyResponse = apiExplorerService.w01d02verifyCall(textToSend, currentMsgId);
                
                if (verifyResponse == null) {
                    logger.warn("Failed to get valid response from API. Ending process.");
                    break;
                }

                currentMsgId = verifyResponse.getMsgID();
                String questionFromTheRobot = verifyResponse.getText();

                if (flagService.checkIfFlagInText(questionFromTheRobot)) {
                    logger.info("Flag detected in robot question. Verification process complete.");
                    break;
                }
                logger.info("Robot question (msgID: {}): {}", currentMsgId, questionFromTheRobot);

                String promptForLlm = promptService.w01d02_createVerificationTaskPrompt(questionFromTheRobot);
                logger.debug("Generated prompt for LLM: {}", promptForLlm);

                String llmAnswer = llmAdapter.getChatCompletion(promptForLlm);
                logger.info("LLM Answer: {}", llmAnswer);

                if (llmAnswer == null || llmAnswer.startsWith("Error:")) {
                    logger.error("LLM returned an error or null: {}. Aborting.", llmAnswer);
                    break;
                }
                
                textToSend = llmAnswer.trim();

                if (attempt == maxAttempts - 1) {
                    logger.warn("Max attempts reached. Flag not found.");
                }

            } catch (Throwable t) {
                logger.error("Error during API call: {}", t.getMessage(), t);
                break;
            }
        }
        logger.info("Ending w01d02 verification process.");
    }
}