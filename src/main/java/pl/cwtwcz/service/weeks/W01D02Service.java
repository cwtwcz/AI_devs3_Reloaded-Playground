package pl.cwtwcz.service.weeks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.cwtwcz.adapter.LlmAdapter;
import pl.cwtwcz.dto.week1.day2.VerifyRequestDto;
import pl.cwtwcz.dto.week1.day2.VerifyResponseDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PromptService;

import java.util.Optional;

@Service
public class W01D02Service {
    private static final Logger logger = LoggerFactory.getLogger(W01D02Service.class);

    private final LlmAdapter llmAdapter;
    private final PromptService promptService;
    private final FlagService flagService;
    private final ApiExplorerService apiExplorerService;
    private final String xyzBaseUrl;

    public W01D02Service(
            LlmAdapter llmAdapter,
            PromptService promptService,
            FlagService flagService,
            ApiExplorerService apiExplorerService,
            @Value("${xyz.base.url}") String xyzBaseUrl) {
        this.llmAdapter = llmAdapter;
        this.promptService = promptService;
        this.flagService = flagService;
        this.apiExplorerService = apiExplorerService;
        this.xyzBaseUrl = xyzBaseUrl;
    }

    public String w01d02() {
        String currentMsgId = "0";
        String textToSend = "READY";
        int maxAttempts = 10;

        logger.info("Starting conversation with the robot.");
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                VerifyRequestDto requestDto = new VerifyRequestDto(textToSend, currentMsgId);
                VerifyResponseDto verifyResponse = apiExplorerService.postJsonForObject(xyzBaseUrl + "verify",
                        requestDto,
                        VerifyResponseDto.class);

                currentMsgId = verifyResponse.getMsgID();
                String questionFromTheRobot = verifyResponse.getText();

                Optional<String> flag = flagService.findFlagInText(questionFromTheRobot);
                if (flag.isPresent()) {
                    logger.info("Flag detected in robot question. Verification process complete.");
                    return flag.get();
                }
                logger.info("Robot question (msgID: {}): {}", currentMsgId, questionFromTheRobot);

                String promptForLlm = promptService.w01d02_createVerificationTaskPrompt(questionFromTheRobot);
                logger.info("Generated prompt for LLM: {}", promptForLlm);

                String llmAnswer = llmAdapter.getAnswer(promptForLlm);
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
        return "Flag not found";
    }
}