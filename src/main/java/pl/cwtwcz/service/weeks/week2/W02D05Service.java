package pl.cwtwcz.service.weeks.week2;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.adapter.GroqAdapter;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.PageScraperService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.dto.week2.ArxivRequestDto;
import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class W02D05Service {

    private static final Logger logger = LoggerFactory.getLogger(W02D05Service.class);

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${custom.w02d05.arxiv-article.url}")
    private String arxivArticleUrl;

    @Value("${custom.w02d05.questions-url.template}")
    private String questionsUrlTemplate;

    @Value("${custom.w02d05.cache-dir}")
    private String cacheDir;

    private final OpenAiAdapter openAiAdapter;
    private final GroqAdapter groqAdapter;
    private final ApiExplorerService apiExplorerService;
    private final FileService fileService;
    private final PageScraperService pageScraperService;
    private final PromptService promptService;
    private final FlagService flagService;

    public String w02d05() {
        try {
            // Step 1. Create cache directory
            createCacheDirectories();

            // Step 2. Download and process the article
            String processedContent = processArxivArticle();

            // Step 3. Get questions from centrala
            String questionsText = downloadQuestions();
            Map<String, String> questions = parseQuestions(questionsText);

            // Step 4. Answer questions using LLM
            Map<String, String> answers = answerQuestions(questions, processedContent);

            // Step 5. Send answers to centrala
            String result = sendAnswersToCentrala(answers);

            logger.info("W02D05 task completed successfully: {}", result);

            // Step 6. Extract flag from result using FlagService
            return flagService.findFlagInText(result)
                    .orElseThrow(() -> new RuntimeException("No flag found in the response from centrala"));

        } catch (Exception e) {
            logger.error("Error in W02D05 task: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    private void createCacheDirectories() {
        // Step 1. Create main cache directory using FileService
        fileService.createDirectories(cacheDir);

        // Step 2. Create subdirectories for images and audio using FileService
        fileService.createDirectories(cacheDir + "images/");
        fileService.createDirectories(cacheDir + "audio/");
    }

    private String processArxivArticle() {
        // Step 1. Download HTML content
        String htmlContent = downloadHtmlContent();
        StringBuilder markdownContent = new StringBuilder();

        // Step 2. Process text content - extract text from HTML tags using
        // PageScraperService
        markdownContent.append("# Artykuł Profesora Maja\n\n");
        String textContent = pageScraperService.extractTextFromHtml(htmlContent);
        markdownContent.append(textContent).append("\n\n");

        // Step 3. Process images using PageScraperService
        List<String> imageSources = pageScraperService.extractImageSources(htmlContent);
        for (String imgSrc : imageSources) {
            if (!imgSrc.isEmpty()) {
                String imageDescription = processImage(imgSrc, "");
                markdownContent.append("**Obraz:** ").append(imageDescription).append("\n\n");
            }
        }

        // Step 4. Process audio files using PageScraperService
        List<String> audioSources = pageScraperService.extractAudioSources(htmlContent);
        for (String audioSrc : audioSources) {
            if (!audioSrc.isEmpty()) {
                String audioTranscription = processAudio(audioSrc);
                markdownContent.append("**Nagranie audio:** ").append(audioTranscription).append("\n\n");
            }
        }

        // Step 5. Save processed content using FileService
        String processedContent = markdownContent.toString();
        fileService.writeStringToFile(cacheDir + "processed_article.md", processedContent);

        return processedContent;
    }

    private String downloadHtmlContent() {
        // Step 1. Download HTML content using ApiExplorerService
        logger.info("Downloading HTML content from: {}", arxivArticleUrl);
        return apiExplorerService.postJsonForObject(arxivArticleUrl, null, String.class);
    }

    private String processImage(String imageSrc, String altText) {
        try {
            // Step 1. Create full URL if relative
            String fullImageUrl = imageSrc.startsWith("http") ? imageSrc
                    : "https://c3ntrala.ag3nts.org/dane/" + imageSrc;

            // Step 2. Generate cache filename
            String filename = "image_" + Math.abs(fullImageUrl.hashCode()) + ".jpg";
            String cachedImagePath = cacheDir + "images/" + filename;
            String cachedDescriptionPath = cacheDir + "images/" + filename + "_description.txt";

            // Step 3. Check if description already exists in cache using FileService
            if (fileService.fileExists(cachedDescriptionPath)) {
                return fileService.readStringFromFile(cachedDescriptionPath);
            }

            // Step 4. Download image using FileService
            fileService.downloadFile(fullImageUrl, cachedImagePath);

            // Step 5. Convert to base64 and get description from LLM using PromptService
            String base64Image = fileService.readFileAsBase64(cachedImagePath);
            String prompt = promptService.w02d05_createImageDescriptionPrompt(altText);

            OpenAiImagePromptRequestDto requestDto = OpenAiImagePromptRequestDto.build("gpt-4o-mini", prompt,
                    base64Image);
            String description = openAiAdapter.getAnswerWithImageRequestPayload(requestDto, "gpt-4o-mini");

            // Step 6. Cache description using FileService
            fileService.writeStringToFile(cachedDescriptionPath, description);

            logger.info("Processed image: {} -> {}", fullImageUrl,
                    description.substring(0, Math.min(100, description.length())));
            return description;

        } catch (Exception e) {
            logger.error("Failed to process image: {}", imageSrc, e);
            return "Błąd przetwarzania obrazu: " + imageSrc;
        }
    }

    private String processAudio(String audioSrc) {
        try {
            // Step 1. Create full URL if relative
            String fullAudioUrl = audioSrc.startsWith("http") ? audioSrc
                    : "https://c3ntrala.ag3nts.org/dane/" + audioSrc;

            // Step 2. Generate cache filename
            String filename = "audio_" + Math.abs(fullAudioUrl.hashCode()) + ".mp3";
            String cachedAudioPath = cacheDir + "audio/" + filename;
            String cachedTranscriptionPath = cacheDir + "audio/" + filename + "_transcription.txt";

            // Step 3. Check if transcription already exists in cache using FileService
            if (fileService.fileExists(cachedTranscriptionPath)) {
                return fileService.readStringFromFile(cachedTranscriptionPath);
            }

            // Step 4. Download audio file using FileService
            fileService.downloadFile(fullAudioUrl, cachedAudioPath);

            // Step 5. Transcribe audio using GroqAdapter
            String transcription = groqAdapter.speechToText(cachedAudioPath, "pl");

            // Step 6. Cache transcription using FileService
            fileService.writeStringToFile(cachedTranscriptionPath, transcription);

            logger.info("Processed audio: {} -> {}", fullAudioUrl,
                    transcription.substring(0, Math.min(100, transcription.length())));
            return transcription;

        } catch (Exception e) {
            logger.error("Failed to process audio: {}", audioSrc, e);
            return "Błąd przetwarzania nagrania: " + audioSrc;
        }
    }

    private String downloadQuestions() {
        // Step 1. Build questions URL with API key
        String questionsUrl = String.format(questionsUrlTemplate, apiKey);

        // Step 2. Download questions using ApiExplorerService
        logger.info("Downloading questions from: {}", questionsUrl);
        return apiExplorerService.postJsonForObject(questionsUrl, null, String.class);
    }

    private Map<String, String> parseQuestions(String questionsText) {
        Map<String, String> questions = new LinkedHashMap<>();

        // Step 1. Create pattern to parse questions in format: ID=question
        Pattern pattern = Pattern.compile("(\\d+)=(.+)");
        String[] lines = questionsText.split("\n");

        // Step 2. Parse each line to extract question ID and text
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    String question = matcher.group(2);
                    questions.put(id, question);
                    logger.info("Found question {}: {}", id, question);
                }
            }
        }

        return questions;
    }

    private Map<String, String> answerQuestions(Map<String, String> questions, String articleContent) {
        Map<String, String> answers = new LinkedHashMap<>();

        // Step 1. Create prompt for all questions at once using PromptService
        String prompt = promptService.w02d05_createMultipleQuestionsPrompt(articleContent, questions);

        logger.info("Sending {} questions to LLM in one request", questions.size());

        // Step 2. Get answers from LLM for all questions at once
        String llmResponse = openAiAdapter.getAnswer(prompt, "gpt-4.1");

        logger.info("Received response from LLM: {}", llmResponse);

        // Step 3. Parse the response to extract individual answers
        answers = parseMultipleAnswers(llmResponse, questions.keySet());

        // Step 4. Log individual answers
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            logger.info("Answer {}: {}", entry.getKey(), entry.getValue());
        }

        return answers;
    }

    private Map<String, String> parseMultipleAnswers(String llmResponse, Set<String> questionIds) {
        Map<String, String> answers = new LinkedHashMap<>();

        // Step 1. Split response into lines
        String[] lines = llmResponse.split("\n");

        // Step 2. Parse each line looking for pattern "ID: answer"
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty())
                continue;

            // Look for pattern like "01: answer" or "1: answer"
            Pattern answerPattern = Pattern.compile("^(\\d+):\\s*(.+)$");
            Matcher matcher = answerPattern.matcher(line);

            if (matcher.find()) {
                String questionId = matcher.group(1);
                String answer = matcher.group(2).trim();

                // Ensure the question ID exists in our original questions
                if (questionIds.contains(questionId)) {
                    answers.put(questionId, answer);
                    logger.debug("Parsed answer for question {}: {}", questionId, answer);
                }
            }
        }

        // Step 3. Check if we got answers for all questions
        if (answers.size() != questionIds.size()) {
            logger.warn("Expected {} answers but got {}. Missing answers for some questions.",
                    questionIds.size(), answers.size());

            // Fill missing answers with default response
            for (String questionId : questionIds) {
                if (!answers.containsKey(questionId)) {
                    answers.put(questionId, "Nie znaleziono odpowiedzi w artykule");
                    logger.warn("No answer found for question {}, using default response", questionId);
                }
            }
        }

        return answers;
    }

    private String sendAnswersToCentrala(Map<String, String> answers) {
        return apiExplorerService.postJsonForObject(reportUrl, new ArxivRequestDto("arxiv", apiKey, answers),
                String.class);
    }
}