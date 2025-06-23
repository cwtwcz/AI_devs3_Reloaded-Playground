package pl.cwtwcz.service.weeks.week5;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.dto.week5.W05D03PasswordRequestDto;
import pl.cwtwcz.dto.week5.W05D03SignRequestDto;
import pl.cwtwcz.dto.week5.W05D03SignResponseDto;
import pl.cwtwcz.dto.week5.W05D03FinalRequestDto;
import pl.cwtwcz.dto.week5.W05D03ChallengeDataDto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@RequiredArgsConstructor
public class W05D03Service {

    private static final Logger logger = LoggerFactory.getLogger(W05D03Service.class);

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${centrala.base.url}")
    private String centralaBaseUrl;

    private static final String RAFAL_ENDPOINT = "https://rafal.ag3nts.org/b46c3";
    private static final String PASSWORD = "NONOMNISMORIAR";

    private final OpenAiAdapter openAiAdapter;
    private final ApiExplorerService apiExplorerService;
    private final PromptService promptService;
    private final FlagService flagService;
    private final ObjectMapper objectMapper;

    // Thread pool for parallel processing
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public String w05d03() {
        try {
            logger.info("Starting W05D03 task - Fast challenge solver");

            // Step 1. Send password to get hash
            String hash = sendPasswordAndGetHash();
            logger.info("Got hash from password: {}", hash);

            // Step 2. Sign the hash to get signature, timestamp and challenges
            W05D03SignResponseDto.W05D03MessageDto signResponse = signHashAndGetChallenges(hash);
            logger.info("Got signature: {}, timestamp: {}, challenges: {}",
                    signResponse.getSignature(), signResponse.getTimestamp(), signResponse.getChallenges());

            // Step 3. Solve challenges quickly
            List<String> allAnswers = solveChallenges(signResponse.getChallenges());
            logger.info("Solved all challenges. Total answers: {}", allAnswers.size());

            // Step 4. Send final answer
            String finalResponse = sendFinalAnswer(signResponse, allAnswers);
            logger.info("Final response: {}", finalResponse);

            return flagService.findFlagInText(finalResponse)
                    .orElse("Task completed. Response: " + finalResponse);

        } catch (Exception e) {
            logger.error("Error in w05d03 task: ", e);
            return "Error: " + e.getMessage();
        }
    }

    private String sendPasswordAndGetHash() {
        // Step 1.1. Create password request
        W05D03PasswordRequestDto passwordRequest = new W05D03PasswordRequestDto(PASSWORD);

        // Step 1.2. Send password and get response with hash
        JsonNode response = apiExplorerService.postJsonForObject(RAFAL_ENDPOINT, passwordRequest, JsonNode.class);

        // Step 1.3. Extract hash from message field
        return response.get("message").asText();
    }

    private W05D03SignResponseDto.W05D03MessageDto signHashAndGetChallenges(String hash) {
        // Step 2.1. Create sign request with hash
        W05D03SignRequestDto signRequest = new W05D03SignRequestDto(hash);

        // Step 2.2. Send sign request and get response with signature, timestamp and
        // challenges
        W05D03SignResponseDto response = apiExplorerService.postJsonForObject(RAFAL_ENDPOINT, signRequest,
                W05D03SignResponseDto.class);

        // Step 2.3. Return message containing signature, timestamp and challenges
        return response.getMessage();
    }

    private List<String> solveChallenges(List<String> challengeUrls) {
        logger.info("Starting parallel processing of {} challenges", challengeUrls.size());

        // Step 3.1. Create parallel tasks for all challenges
        List<CompletableFuture<String>> futures = IntStream.range(0, challengeUrls.size())
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String url = challengeUrls.get(i);
                        logger.info("Processing challenge {} from URL: {}", i + 1, url);

                        // Step 3.1.1. Download challenge data
                        W05D03ChallengeDataDto challengeData = apiExplorerService.postJsonForObject(url, null,
                                W05D03ChallengeDataDto.class);
                        logger.info("Challenge {}: task='{}', data type={}", i + 1, challengeData.getTask(),
                                challengeData.getData().getClass().getSimpleName());

                        // Step 3.1.2. Solve the challenge using AI
                        String answer = solveChallenge(challengeData);
                        logger.info("Challenge {} solved: {}", i + 1, answer);

                        return answer;

                    } catch (Exception e) {
                        logger.error("Error solving challenge {}: ", i + 1, e);
                        return "ERROR: " + e.getMessage();
                    }
                }, executorService))
                .toList();

        // Step 3.2. Wait for all futures to complete and collect results
        List<String> allAnswers = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                String answer = futures.get(i).get(); // This will block until the future completes
                allAnswers.add(answer);
            } catch (Exception e) {
                logger.error("Error getting result for challenge {}: ", i + 1, e);
                allAnswers.add("ERROR: " + e.getMessage());
            }
        }

        logger.info("Completed parallel processing of {} challenges", allAnswers.size());
        return allAnswers;
    }

    private String solveChallenge(W05D03ChallengeDataDto challengeData) {
        // Step 3.2.1. Convert data to string for analysis
        String dataContent = challengeData.getData().toString();

        // Step 3.2.2. Check if task contains URL for knowledge source and download in
        // parallel
        CompletableFuture<String> knowledgeSourceFuture = null;
        if (containsUrl(challengeData.getTask())) {
            String url = extractUrl(challengeData.getTask());
            logger.info("Found knowledge source URL in task: {}", url);

            knowledgeSourceFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    String knowledge = apiExplorerService.postJsonForObject(url, null, String.class);
                    logger.info("Downloaded knowledge source ({} characters)", knowledge.length());
                    return knowledge;
                } catch (Exception e) {
                    logger.warn("Failed to download knowledge source from {}: {}", url, e.getMessage());
                    return null;
                }
            }, executorService);
        }

        // Step 3.2.3. Get knowledge source if available
        String knowledgeSource = null;
        if (knowledgeSourceFuture != null) {
            try {
                knowledgeSource = knowledgeSourceFuture.get(); // Wait for knowledge source
            } catch (Exception e) {
                logger.warn("Error getting knowledge source: {}", e.getMessage());
            }
        }

        // Step 3.2.4. Use AI to analyze task and generate answer
        logger.info("Data content: {}", dataContent);
        String prompt = promptService.w05d03_createChallengeAnalysisPrompt(challengeData.getTask(), dataContent,
                knowledgeSource);
        String answer = openAiAdapter.getAnswer(prompt, "gpt-4.1-nano");
        logger.info("Answer: {}", answer);

        // Step 3.2.5. Clean up answer (remove any extra formatting)
        return answer.trim();
    }

    private boolean containsUrl(String text) {
        return text != null && (text.contains("http://") || text.contains("https://"));
    }

    private String extractUrl(String text) {
        // Extract URL from text - find first occurrence of http:// or https://
        String[] parts = text.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("http://") || part.startsWith("https://")) {
                // Remove any trailing punctuation
                return part.replaceAll("[,.;:!?]+$", "");
            }
        }
        return null;
    }

    private String sendFinalAnswer(W05D03SignResponseDto.W05D03MessageDto signResponse, List<String> answers) {
        // Step 4.1. Create final request with all required fields
        W05D03FinalRequestDto finalRequest = new W05D03FinalRequestDto(
                apiKey,
                signResponse.getTimestamp(),
                signResponse.getSignature(),
                answers);

        // Step 4.2. Send final answer and get response
        return apiExplorerService.postJsonForObject(RAFAL_ENDPOINT, finalRequest, String.class);
    }
}