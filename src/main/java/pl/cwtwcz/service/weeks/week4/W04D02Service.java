package pl.cwtwcz.service.weeks.week4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.week4.ResearchVerificationRequestDto;
import pl.cwtwcz.dto.week4.ResearchVerificationResponseDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.PromptService;

import static java.util.Arrays.asList;

import java.util.List;

@RequiredArgsConstructor
@Service
public class W04D02Service {

    private static final Logger logger = LoggerFactory.getLogger(W04D02Service.class);

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.w04d02.model.name}")
    private String fineTuneModelName;

    private final ApiExplorerService apiExplorerService;
    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;
    private final FlagService flagService;
    private final FileService fileService;

    public String w04d02() {
        try {
            // Step 1. Read the verify.txt file
            logger.info("Step 1: Reading verify.txt file");
            String verifyFileContent = fileService
                    .readStringFromFile("src/main/resources/inputfiles/w04d02/verify.txt");

            // Step 2. Process each line to verify correctness
            logger.info("Step 2: Processing research data for verification");
            List<String> correctIds = processResearchData(verifyFileContent);

            // Step 3. Send results to centrala
            logger.info("Step 3: Sending results to centrala with {} correct IDs", correctIds.size());
            ResearchVerificationResponseDto response = sendVerificationResults(correctIds);

            return flagService.findFlagInText(response.getMessage()).orElse(response.getMessage());
        } catch (Exception e) {
            logger.error("Error processing W04D02 research verification task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process W04D02 research verification task", e);
        }
    }

    private List<String> processResearchData(String fileContent) {
        return asList(fileContent.split("\n")).stream()
                .map(line -> line.split("=", 2))
                .filter(parts -> parts.length == 2 && verifyResearchData(parts[1].trim()))
                .peek(parts -> logger.info("Research entry {} is CORRECT", parts[0].trim()))
                .map(parts -> parts[0].trim())
                .toList();
    }

    private boolean verifyResearchData(String researchData) {
        try {
            String verificationPrompt = promptService.w04d02_createResearchVerificationPrompt(researchData);
            String response = openAiAdapter.getAnswer(verificationPrompt, fineTuneModelName);
            logger.info("Verification response for '{}': {}", researchData, response);
            return response.equals("1");
        } catch (Exception e) {
            logger.error("Error verifying research data '{}': {}", researchData, e.getMessage(), e);
            return false;
        }
    }

    private ResearchVerificationResponseDto sendVerificationResults(List<String> correctIds) {
        ResearchVerificationRequestDto request = new ResearchVerificationRequestDto("research", apiKey, correctIds);
        logger.info("Sending verification results: {}", correctIds);
        return apiExplorerService.postJsonForObject(reportUrl, request, ResearchVerificationResponseDto.class);
    }
}