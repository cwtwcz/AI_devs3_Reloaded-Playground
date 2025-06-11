package pl.cwtwcz.service.weeks.week4;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.week4.PageLinkDto;
import pl.cwtwcz.dto.week4.SoftoAnswersDto;
import pl.cwtwcz.dto.week4.SoftoQuestionsDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PageScraperService;
import pl.cwtwcz.service.PromptService;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class W04D03Service {

    private static final Logger logger = LoggerFactory.getLogger(W04D03Service.class);
    private static final int MAX_DEPTH = 10;

    @Value("${aidevs.api.key}")
    private String aiDevsApiKey;

    @Value("${custom.w04d03.questions-url.template}")
    private String questionsUrlTemplate;

    @Value("${custom.w04d03.softo.base.url}")
    private String softoBaseUrl;

    @Value("${custom.report.url}")
    private String reportUrl;

    private final RestTemplate restTemplate;
    private final ApiExplorerService apiExplorerService;
    private final PageScraperService pageScraperService;
    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;
    private final FlagService flagService;

    private final Pattern LINK_PATTERN = Pattern.compile("<a[^>]*href=[\"']([^\"']*)[\"'][^>]*>([^<]*)</a>",
            CASE_INSENSITIVE);

    public String w04d03() {
        try {
            // Step 1. Fetch questions from centrala
            logger.info("Step 1. Fetching questions from centrala");
            Map<String, String> questions = fetchQuestionsFromCentrala();
            logger.info("Retrieved {} questions: {}", questions.size(), questions.keySet());

            // Step 2. Create universal search mechanism for each question
            Map<String, String> answers = new HashMap<>();

            for (Map.Entry<String, String> questionEntry : questions.entrySet()) {
                String questionId = questionEntry.getKey();
                String question = questionEntry.getValue();

                logger.info("Step 2. Processing question {}: {}", questionId, question);

                String answer = searchForAnswer(question);
                answers.put(questionId, answer);

                logger.info("Found answer for {}: {}", questionId, answer);
            }

            // Step 3. Send answers to centrala
            logger.info("Step 3. Sending answers to centrala");
            SoftoAnswersDto answerDto = new SoftoAnswersDto("softo", aiDevsApiKey, answers);
            String response = apiExplorerService.postJsonForObject(reportUrl, answerDto, String.class);

            logger.info("Final response from centrala: {}", response);

            // Step 4. Search for flag in response
            return flagService.findFlagInText(response)
                    .orElse("No flag found in response: " + response);

        } catch (Exception e) {
            logger.error("Error in W04D03 task: {}", e.getMessage(), e);
            throw new RuntimeException("W04D03 task failed: " + e.getMessage(), e);
        }
    }

    private Map<String, String> fetchQuestionsFromCentrala() {
        String questionsUrl = String.format(questionsUrlTemplate, aiDevsApiKey);
        logger.info("Fetching questions from: {}", questionsUrl);

        try {
            SoftoQuestionsDto questionsDto = restTemplate.getForObject(questionsUrl, SoftoQuestionsDto.class);
            if (questionsDto == null || questionsDto.isEmpty()) {
                throw new RuntimeException("Failed to fetch questions from centrala");
            }
            return questionsDto;
        } catch (Exception e) {
            logger.error("Error fetching questions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch questions from centrala: " + e.getMessage(), e);
        }
    }

    private String searchForAnswer(String question) {
        Set<String> visitedUrls = new HashSet<>();
        String currentUrl = softoBaseUrl;

        return searchRecursively(question, currentUrl, visitedUrls, 0)
                .orElse("Odpowiedź nie została znaleziona");
    }

    private Optional<String> searchRecursively(String question, String currentUrl, Set<String> visitedUrls, int depth) {
        if (depth >= MAX_DEPTH || visitedUrls.contains(currentUrl)) {
            return Optional.empty();
        }
        visitedUrls.add(currentUrl);
        logger.info("Step {}. Visiting URL: {}", depth + 1, currentUrl);

        try {
            String pageContent = restTemplate.getForObject(currentUrl, String.class);
            if (pageContent == null) {
                logger.warn("Empty response from URL: {}", currentUrl);
                return Optional.empty();
            }

            // Extract both clean text and preserve link information
            String cleanContent = pageScraperService.extractTextFromHtml(pageContent);
            String content = enrichContentWithLinks(pageContent, cleanContent);
            logger.info("Extracted {} characters of clean text, {} characters enriched", cleanContent.length(),
                    content.length());

            // Step 1. Check if answer exists on current page
            String hasAnswer = openAiAdapter.getAnswer(
                    promptService.w04d03_createAnswerCheckPrompt(content, question));
            if ("TAK".equalsIgnoreCase(hasAnswer.trim())) {
                logger.info("Answer found on current page, extracting...");

                // Extract the answer
                String extractPrompt = promptService.w04d03_createAnswerExtractionPrompt(content, question);
                String answer = openAiAdapter.getAnswer(extractPrompt);

                logger.info("Extracted answer: {}", answer);
                return Optional.of(answer.trim());
            }

            // Step 2. No answer found, select next link to follow
            logger.info("No answer on current page, selecting next link to follow");

            List<PageLinkDto> availableLinks = extractLinksFromPage(pageContent, currentUrl);
            if (availableLinks.isEmpty()) {
                logger.info("No links found on page, ending search");
                return Optional.empty();
            }

            // Filter out already visited links
            availableLinks.removeIf(link -> visitedUrls.contains(link.getUrl()));
            if (availableLinks.isEmpty()) {
                logger.info("All links already visited, ending search");
                return Optional.empty();
            }

            // Use LLM to select the best link
            String linksText = formatLinksForPrompt(availableLinks);
            String linkSelectionPrompt = promptService.w04d03_createLinkSelectionPrompt(content, question, linksText);
            String selectedUrl = openAiAdapter.getAnswer(linkSelectionPrompt).trim();

            // Validate selected URL
            if (!isValidUrl(selectedUrl, availableLinks)) {
                logger.warn("Invalid URL selected by LLM: {}, using first available link", selectedUrl);
                selectedUrl = availableLinks.get(0).getUrl();
            }

            logger.info("Selected URL: {}", selectedUrl);

            // Continue search recursively
            return searchRecursively(question, selectedUrl, visitedUrls, depth + 1);
        } catch (Exception e) {
            logger.error("Error processing URL {}: {}", currentUrl, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private List<PageLinkDto> extractLinksFromPage(String pageContent, String baseUrl) {
        List<PageLinkDto> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(pageContent);

        while (matcher.find()) {
            String href = matcher.group(1);
            String linkText = matcher.group(2);

            if (href == null || href.isEmpty() || href.startsWith("#") || href.startsWith("javascript:")) {
                continue;
            }

            // Convert relative URLs to absolute
            String absoluteUrl = resolveUrl(href, baseUrl);

            // Only include links within the softo domain
            if (absoluteUrl.startsWith(softoBaseUrl)) {
                links.add(new PageLinkDto(absoluteUrl, linkText.trim(), ""));
            }
        }

        logger.info("Extracted {} valid links from page", links.size());
        return links;
    }

    private String resolveUrl(String href, String baseUrl) {
        try {
            if (href.startsWith("http")) {
                return href;
            }

            URI baseUri = new URI(baseUrl);
            URI resolvedUri = baseUri.resolve(href);
            return resolvedUri.toString();
        } catch (Exception e) {
            logger.warn("Failed to resolve URL: {} with base: {}", href, baseUrl);
            return href;
        }
    }

    private String formatLinksForPrompt(List<PageLinkDto> links) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < links.size(); i++) {
            PageLinkDto link = links.get(i);
            sb.append(String.format("%d. URL: %s | Text: %s\n", i + 1, link.getUrl(), link.getText()));
        }
        return sb.toString();
    }

    private boolean isValidUrl(String selectedUrl, List<PageLinkDto> availableLinks) {
        return availableLinks.stream()
                .anyMatch(link -> link.getUrl().equals(selectedUrl));
    }

    private String enrichContentWithLinks(String htmlContent, String cleanText) {
        StringBuilder enrichedContent = new StringBuilder();
        enrichedContent.append("TREŚĆ STRONY:\n").append(cleanText).append("\n\n");

        // Extract all links with their context
        Pattern linkPattern = Pattern.compile("<a[^>]*href=[\"']([^\"']*)[\"'][^>]*>([^<]*)</a>", CASE_INSENSITIVE);
        Matcher matcher = linkPattern.matcher(htmlContent);

        List<String> foundLinks = new ArrayList<>();
        while (matcher.find()) {
            String url = matcher.group(1);
            String linkText = matcher.group(2);

            if (url != null && !url.isEmpty() && !url.startsWith("#") && !url.startsWith("javascript:")) {
                // Convert relative URLs to absolute
                String absoluteUrl = resolveUrl(url, softoBaseUrl);
                foundLinks.add(String.format("LINK: %s (tekst: %s)", absoluteUrl, linkText.trim()));
            }
        }

        if (!foundLinks.isEmpty()) {
            enrichedContent.append("ZNALEZIONE LINKI NA STRONIE:\n");
            for (String link : foundLinks) {
                enrichedContent.append(link).append("\n");
            }
        }

        return enrichedContent.toString();
    }
}