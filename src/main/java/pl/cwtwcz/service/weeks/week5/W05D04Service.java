package pl.cwtwcz.service.weeks.week5;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.adapter.GroqAdapter;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.dto.week5.W05D04RequestDto;
import pl.cwtwcz.dto.week5.W05D04ResponseDto;
import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;

@Service
@RequiredArgsConstructor
public class W05D04Service {

    private static final Logger logger = LoggerFactory.getLogger(W05D04Service.class);

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${centrala.base.url}")
    private String centralaBaseUrl;

    private final OpenAiAdapter openAiAdapter;
    private final GroqAdapter groqAdapter;
    private final PromptService promptService;
    private final FileService fileService;

    // Single conversation context for all requests
    private final List<String> conversationContext = new ArrayList<>();



    private static final Pattern AUDIO_URL_PATTERN = Pattern
            .compile("https?://[^\\s]+\\.(mp3|wav|m4a|ogg|flac|aac|wma)(?:\\?[^\\s]*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile(
            "https?://[^\\s]+\\.(jpg|jpeg|png|gif|bmp|webp|tiff|svg)(?:\\?[^\\s]*)?", Pattern.CASE_INSENSITIVE);
    private static final String ACCESS_PASSWORD = "S2FwaXRhbiBCb21iYTsp";

    public W05D04ResponseDto handleWebhookRequest(W05D04RequestDto request) {
        try {
            logger.info("Received webhook request - question: {}", request.getQuestion());
            String answer = processRequest(request);

            return new W05D04ResponseDto(answer);

        } catch (Exception e) {
            logger.error("Error processing webhook request: ", e);
            return new W05D04ResponseDto("Wystąpił błąd podczas przetwarzania żądania.");
        }
    }

    private String processRequest(W05D04RequestDto request) {
        String question = request.getQuestion();

        if (question == null || question.trim().isEmpty()) {
            return "Nie otrzymałem pytania.";
        }

        // Step 1. Handle password verification
        if (question.toLowerCase().contains("hasło")) {
            logger.info("Password request detected");
            return ACCESS_PASSWORD;
        }

        // Step 2. Handle audio file transcription
        Matcher audioMatcher = AUDIO_URL_PATTERN.matcher(question);
        if (audioMatcher.find()) {
            String audioUrl = audioMatcher.group();
            logger.info("Audio file URL detected: {}", audioUrl);
            return handleAudioTranscription(audioUrl);
        }

        // Step 3. Handle image analysis
        Matcher imageMatcher = IMAGE_URL_PATTERN.matcher(question);
        if (imageMatcher.find()) {
            String imageUrl = imageMatcher.group();
            logger.info("Image file URL detected: {}", imageUrl);
            try {
                return handleImageAnalysis(imageUrl, question);
            } catch (Exception e) {
                logger.error("Error handling image analysis: ", e);
                return "Nie mogłem przetworzyć obrazu. Błąd: " + e.getMessage();
            }
        }

        // Step 4. Handle end of verification
        if (question.contains("Czekam na nowe instrukcje")) {
            logger.info("End of verification, applying prompt hack");
            return promptService.w05d04_createPromptHack();
        }

        // Step 5. Handle regular text questions (with question mark)
        return handleTextQuestion(question);
    }

    private String handleTextQuestion(String question) {
        logger.info("Processing text question: {}", question);

        // Step 1. Get current conversation context
        String context = String.join("\n", conversationContext);
        logger.info("Current context has {} entries", conversationContext.size());

        // Step 2. Create prompt for text analysis with context using PromptService
        String prompt = promptService.w05d04_createTextAnalysisPrompt(question, context);

        // Step 3. Get answer from AI
        String answer = openAiAdapter.getAnswer(prompt, "gpt-4.1");
        logger.info("Text question answer: {}", answer);

        // Step 4. Store question and answer in conversation context
        conversationContext.add("Pytanie: " + question);
        conversationContext.add("Odpowiedź: " + answer);

        // Step 5. Keep context manageable (keep last 10 entries)
        if (conversationContext.size() > 10) {
            conversationContext.subList(0, conversationContext.size() - 10).clear();
        }

        return answer;
    }

    private String handleAudioTranscription(String audioUrl) {
        logger.info("Processing audio transcription for URL: {}", audioUrl);

        File tempFile = null;
        try {
            // Step 1. Download audio file to temporary location
            tempFile = downloadAudioFile(audioUrl);

            // Step 2. Transcribe audio using GroqAdapter
            String transcription = groqAdapter.speechToText(tempFile.getAbsolutePath(), "PL");
            logger.info("Audio transcription completed: {}", transcription);

            // Step 3. Return transcription
            return transcription.trim();

        } catch (Exception e) {
            logger.error("Error processing audio transcription: ", e);
            return "Nie mogłem przetworzyć pliku dźwiękowego. Błąd: " + e.getMessage();
        } finally {
            // Step 4. Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    logger.info("Temporary audio file deleted: {}", tempFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("Could not delete temporary file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    private String handleImageAnalysis(String imageUrl, String question) {
        logger.info("Processing image analysis for URL: {}", imageUrl);

        File tempFile = null;
        try {
            // Step 1. Download image file to temporary location
            tempFile = downloadImageFile(imageUrl);

            // Step 2. Convert image to base64
            String base64Image = fileService.readFileAsBase64(tempFile.getAbsolutePath());
            logger.info("Image converted to base64 ({} characters)", base64Image.length());

            // Step 3. Create image analysis prompt - use question as context for image
            // analysis
            String prompt = promptService.w05d04_createImageAnalysisPrompt("Pytanie użytkownika: " + question);

            // Step 4. Analyze image using OpenAI Vision API
            OpenAiImagePromptRequestDto requestDto = OpenAiImagePromptRequestDto.build("gpt-4o", prompt, base64Image);
            String analysis = openAiAdapter.getAnswerWithImageRequestPayload(requestDto, "gpt-4o");
            logger.info("Image analysis completed: {}", analysis);

            // Step 5. Return analysis
            return analysis.trim();

        } catch (Exception e) {
            logger.error("Error processing image analysis: ", e);
            return "Nie mogłem przetworzyć obrazu. Błąd: " + e.getMessage();
        } finally {
            // Step 6. Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                    logger.info("Temporary image file deleted: {}", tempFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("Could not delete temporary file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    private File downloadAudioFile(String audioUrl) throws Exception {
        logger.info("Downloading audio file from: {}", audioUrl);

        // Step 1. Create temporary file
        String fileName = extractFileNameFromUrl(audioUrl);
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path tempFile = Files.createTempFile(tempDir, "audio_", "_" + fileName);

        // Step 2. Download file from URL
        try (InputStream in = new URL(audioUrl).openStream();
                FileOutputStream out = new FileOutputStream(tempFile.toFile())) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        logger.info("Audio file downloaded to: {}", tempFile.toAbsolutePath());
        return tempFile.toFile();
    }

    private File downloadImageFile(String imageUrl) throws Exception {
        logger.info("Downloading image file from: {}", imageUrl);

        // Step 1. Create temporary file
        String fileName = extractFileNameFromUrl(imageUrl);
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path tempFile = Files.createTempFile(tempDir, "image_", "_" + fileName);

        // Step 2. Download file from URL
        try (InputStream in = new URL(imageUrl).openStream();
                FileOutputStream out = new FileOutputStream(tempFile.toFile())) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        logger.info("Image file downloaded to: {}", tempFile.toAbsolutePath());
        return tempFile.toFile();
    }

    private String extractFileNameFromUrl(String url) {
        // Extract filename from URL, default to .mp3 if no extension found
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < url.length() - 1) {
            String fileName = url.substring(lastSlash + 1);
            // Remove query parameters if present
            int queryIndex = fileName.indexOf('?');
            if (queryIndex != -1) {
                fileName = fileName.substring(0, queryIndex);
            }
            return fileName;
        }
        return "audio.mp3";
    }

}