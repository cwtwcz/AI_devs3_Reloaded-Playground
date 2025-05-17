package pl.cwtwcz.service.weeks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pl.cwtwcz.adapter.LlmAdapter;
import pl.cwtwcz.service.PageScraperService;
import pl.cwtwcz.service.PromptService;

@Service
public class W01D01Service {
    private static final Logger logger = LoggerFactory.getLogger(W01D01Service.class);

    private final PageScraperService pageExplorerService;
    private final LlmAdapter llmAdapter;
    private final PromptService promptService;

    public W01D01Service(
            PageScraperService pageExplorerService,
            LlmAdapter llmAdapter,
            PromptService promptService) {
        this.pageExplorerService = pageExplorerService;
        this.llmAdapter = llmAdapter;
        this.promptService = promptService;
    }

    public String execute() {
        String question = pageExplorerService.getQuestionFromLoginPage();
        logger.info("Question from Page: {}", question);

        String prompt = promptService.w01d01_createYearExtractionPrompt(question);
        logger.info("Prompt to extract year: {}", prompt);

        String llmAnswer = llmAdapter.getAnswer(prompt);
        logger.info("Answer from LLM: {}", llmAnswer);

        return pageExplorerService.loginToRobotsAndFindFlag(llmAnswer);
    }
} 