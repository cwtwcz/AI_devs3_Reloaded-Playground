package pl.cwtwcz.service.weeks.week1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.cwtwcz.adapter.LlmAdapter;
import pl.cwtwcz.dto.common.ReportRequestDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FlagService;

@Service
public class W01D05Service {
    private static final Logger logger = LoggerFactory.getLogger(W01D05Service.class);

    private final LlmAdapter llmAdapter;
    private final PromptService promptService;
    private final ApiExplorerService apiExplorerService;
    private final FlagService flagService;
    private final String aidevsApiKey;
    private final String reportUrl = "https://c3ntrala.ag3nts.org/report";

    public W01D05Service(
            LlmAdapter llmAdapter,
            PromptService promptService,
            ApiExplorerService apiExplorerService,
            FlagService flagService,
            @Value("${aidevs.api.key}") String aidevsApiKey) {
        this.llmAdapter = llmAdapter;
        this.promptService = promptService;
        this.apiExplorerService = apiExplorerService;
        this.flagService = flagService;
        this.aidevsApiKey = aidevsApiKey;
    }

    public String w01d05() {
        // Step 0: Load text from remote source
        String dataUrl = "https://c3ntrala.ag3nts.org/data/" + aidevsApiKey + "/cenzura.txt";
        String text = apiExplorerService.postJsonForObject(dataUrl, null, String.class);
        logger.info("Loaded text from {}: {}", dataUrl, text);

        // Step 1: Create prompt
        String prompt = promptService.w01d05_createCensorPrompt(text);
        logger.info("Generated prompt for LLM: {}", prompt);

        // Step 2: Get censored text from LLM
        String censoredText = llmAdapter.getAnswer(prompt);
        logger.info("Censored text from LLM: {}", censoredText);

        // Step 3: Build DTO
        ReportRequestDto requestDto = new ReportRequestDto("CENZURA", aidevsApiKey, censoredText);

        // Step 4: Send to API
        String response = apiExplorerService.postJsonForObject(reportUrl, requestDto, String.class);
        logger.info("Response from report API: {}", response);

        // Check for flag in response
        return flagService.findFlagInText(response)
                .orElseThrow(() -> new RuntimeException("No flag found in the response from the report API."));
    }
}
