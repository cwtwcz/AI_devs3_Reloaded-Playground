package pl.cwtwcz.service.weeks.week1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.week1.day2.VerifyTextMsgDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PromptService;

import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class W01D02Service {

    private static final Logger logger = LoggerFactory.getLogger(W01D02Service.class);

    @Value("${xyz.base.url}")
    private String xyzBaseUrl;

    private final OpenAiAdapter llmAdapter;
    private final PromptService promptService;
    private final FlagService flagService;
    private final ApiExplorerService apiExplorerService;

    public String w01d02() {
        String currentMsgId = "0";
        String textToSend = "READY";
        int maxAttempts = 10;

        logger.info("Starting conversation with the robot.");
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                VerifyTextMsgDto requestDto = new VerifyTextMsgDto(textToSend, currentMsgId);
                VerifyTextMsgDto verifyResponse = apiExplorerService.postJsonForObject(xyzBaseUrl + "verify",
                        requestDto, VerifyTextMsgDto.class);

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