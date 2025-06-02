package pl.cwtwcz.service.weeks.week3;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.TextAnalysisService;
import pl.cwtwcz.dto.week3.DokumentyRequestDto;

import java.util.*;

@RequiredArgsConstructor
@Service
public class W03D01Service {

    private static final Logger logger = LoggerFactory.getLogger(W03D01Service.class);

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${custom.w03d01.reports-dir}")
    private String reportsDir;

    @Value("${custom.w03d01.facts-dir}")
    private String factsDir;

    private final OpenAiAdapter openAiAdapter;
    private final ApiExplorerService apiExplorerService;
    private final FileService fileService;
    private final PromptService promptService;
    private final FlagService flagService;
    private final TextAnalysisService textAnalysisService;

    public String w03d01() {
        try {
            logger.info("Starting W03D01 task - analyzing reports and facts");

            // Step 1. Read all report and fact files
            Map<String, String> reports = readAllReportFiles();
            Map<String, String> facts = readAllFactFiles();

            // Step 2. Generate comprehensive facts analysis
            String factsAnalysis = generateFactsAnalysis(facts);

            // Step 3. Generate keywords for each report
            Map<String, String> keywordsMap = generateKeywordsForAllReports(reports, facts, factsAnalysis);

            // Step 4. Send solution to API and extract flag
            String response = sendSolutionToAPI(keywordsMap);

            // Step 5. Extract flag from response
            return flagService.findFlagInText(response)
                    .orElse("Task completed. Response: " + response);
        } catch (Exception e) {
            logger.error("Error in w03d01 task: ", e);
            return "Error: " + e.getMessage();
        }
    }

    private Map<String, String> readAllReportFiles() {
        // Step 1.1. Read all report files using FileService
        try {
            Map<String, String> reportFilePaths = fileService.listFilesInDirectory(reportsDir, ".txt", "report");
            Map<String, String> reports = fileService.readMultipleFiles(reportFilePaths);

            for (String filename : reports.keySet()) {
                logger.info("Read report file: {}", filename);
            }

            return reports;
        } catch (Exception e) {
            logger.error("Error reading report files: ", e);
            return new HashMap<>();
        }
    }

    private Map<String, String> readAllFactFiles() {
        // Step 1.2. Read all fact files using FileService
        try {
            Map<String, String> factFilePaths = fileService.listFilesInDirectory(factsDir, ".txt", null);
            Map<String, String> facts = fileService.readMultipleFiles(factFilePaths);

            for (String filename : facts.keySet()) {
                logger.info("Read fact file: {}", filename);
            }

            return facts;
        } catch (Exception e) {
            logger.error("Error reading fact files: ", e);
            return new HashMap<>();
        }
    }

    private String generateFactsAnalysis(Map<String, String> facts) {
        // Step 2.1. Create comprehensive facts analysis using PromptService
        StringBuilder allFacts = new StringBuilder();
        for (Map.Entry<String, String> factEntry : facts.entrySet()) {
            allFacts.append("Plik: ").append(factEntry.getKey()).append("\n");
            allFacts.append(factEntry.getValue()).append("\n\n");
        }

        // Step 2.2. Use PromptService for prompt creation
        String prompt = promptService.w03d01_createFactsAnalysisPrompt(allFacts.toString());
        return openAiAdapter.getAnswer(prompt);
    }

    private Map<String, String> generateKeywordsForAllReports(
            Map<String, String> reports, Map<String, String> facts, String factsAnalysis) {
        // Step 3.1. Process each report to generate keywords
        Map<String, String> results = new HashMap<>();

        for (Map.Entry<String, String> reportEntry : reports.entrySet()) {
            String filename = reportEntry.getKey();
            String content = reportEntry.getValue();

            logger.info("Analyzing report: {}", filename);

            // Step 3.2. Extract filename information using TextAnalysisService
            String filenameInfo = textAnalysisService.extractFilenameInformation(filename);

            // Step 3.3. Find related facts using TextAnalysisService
            String relatedFacts = textAnalysisService.findRelatedFactsForReport(content, facts);

            // Step 3.4. Generate keywords using PromptService
            String prompt = promptService.w03d01_createKeywordGenerationPrompt(filename, content, filenameInfo,
                    relatedFacts, factsAnalysis);
            String keywords = openAiAdapter.getAnswer(prompt);

            results.put(filename, keywords);
            logger.info("Generated keywords for {}: {}", filename, keywords);
        }

        return results;
    }

    private String sendSolutionToAPI(Map<String, String> keywords) {
        // Step 4.1. Create DTO for API request
        DokumentyRequestDto requestDto = new DokumentyRequestDto("dokumenty", apiKey, keywords);

        logger.info("Sending solution with {} keyword sets", keywords.size());
        for (Map.Entry<String, String> entry : keywords.entrySet()) {
            logger.info("{}: {}", entry.getKey(), entry.getValue());
        }

        // Step 4.2. Send to API using ApiExplorerService with DTO
        return apiExplorerService.postJsonForObject(reportUrl, requestDto, String.class);
    }
}