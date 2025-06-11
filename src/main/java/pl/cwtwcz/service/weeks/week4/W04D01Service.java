package pl.cwtwcz.service.weeks.week4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;
import pl.cwtwcz.dto.common.ReportRequestDto;
import pl.cwtwcz.dto.week4.PhotoAnalysisRequestDto;
import pl.cwtwcz.dto.week4.PhotoProcessingResponseDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PromptService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class W04D01Service {

    private static final Logger logger = LoggerFactory.getLogger(W04D01Service.class);

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${aidevs.api.key}")
    private String apiKey;

    private final ApiExplorerService apiExplorerService;
    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;
    private final FlagService flagService;

    public String w04d01() {
        try {
            // Step 1. Initialize contact with automation system
            logger.info("Step 1: Initializing contact with automation system");
            PhotoProcessingResponseDto initialResponse = initializeContact();

            // Step 2. Extract photo URLs from the initial response
            logger.info("Step 2: Extracting photo URLs from response");
            List<PhotoAnalysisRequestDto> photoRequests = extractPhotoUrls(initialResponse.getMessage());

            // Step 3. Process each photo iteratively
            logger.info("Step 3: Processing {} photos", photoRequests.size());
            List<String> finalPhotoUrls = photoRequests.stream()
                    .map(photoRequest -> processPhotoIteratively(photoRequest))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Step 4. Create Barbara's description using processed photos
            logger.info("Step 4: Creating Barbara's description from {} processed photos", finalPhotoUrls.size());
            String barbaraDescription = createBarbaraDescription(finalPhotoUrls);

            // Step 5. Send final report
            logger.info("Step 5: Sending final report with Barbara's description");
            PhotoProcessingResponseDto finalResponse = sendFinalReport(barbaraDescription);

            return flagService.findFlagInText(finalResponse.getMessage()).orElse(finalResponse.getMessage());
        } catch (Exception e) {
            logger.error("Error processing Barbara photos task: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process Barbara photos task", e);
        }
    }

    private PhotoProcessingResponseDto initializeContact() {
        ReportRequestDto request = new ReportRequestDto("photos", apiKey, "START");
        return apiExplorerService.postJsonForObject(reportUrl, request, PhotoProcessingResponseDto.class);
    }

    private List<PhotoAnalysisRequestDto> extractPhotoUrls(String message) {
        List<PhotoAnalysisRequestDto> photoRequests = new ArrayList<>();

        // Extract URLs - handle both centrala.ag3nts.org and c3ntrala.ag3nts.org
        Pattern urlPattern = Pattern.compile(
                "https://c?entrala\\.ag3nts\\.org/dane/[^\\s]+\\.(png|jpg|jpeg|PNG|JPG|JPEG)",
                Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = urlPattern.matcher(message);

        while (urlMatcher.find()) {
            String originalUrl = urlMatcher.group();
            // Convert to the correct c3ntrala format if needed
            String url = originalUrl.replace("centrala.ag3nts.org", "c3ntrala.ag3nts.org");
            String filename = extractFilename(url);

            PhotoAnalysisRequestDto request = new PhotoAnalysisRequestDto();
            request.setPhotoUrl(url);
            request.setFilename(filename);
            request.setNeedsProcessing(true);

            photoRequests.add(request);
            logger.info("Found photo URL: {} -> {}", filename, url);
        }

        // If no full URLs found, try to extract filenames and construct URLs
        if (photoRequests.isEmpty()) {
            Pattern filenamePattern = Pattern.compile("([A-Za-z0-9_]+\\.(png|jpg|jpeg|PNG|JPG|JPEG))",
                    Pattern.CASE_INSENSITIVE);
            Matcher filenameMatcher = filenamePattern.matcher(message);

            while (filenameMatcher.find()) {
                String filename = filenameMatcher.group(1);
                String url = "https://c3ntrala.ag3nts.org/dane/barbara/" + filename;

                PhotoAnalysisRequestDto request = new PhotoAnalysisRequestDto();
                request.setPhotoUrl(url);
                request.setFilename(filename);
                request.setNeedsProcessing(true);

                photoRequests.add(request);
                logger.info("Found photo filename: {} -> {}", filename, url);
            }
        }

        return photoRequests;
    }

    private String extractFilename(String url) {
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }

    private String processPhotoIteratively(PhotoAnalysisRequestDto photoRequest) {
        String currentUrl = photoRequest.getPhotoUrl();
        String currentFilename = photoRequest.getFilename();
        int maxIterations = 5; // Prevent infinite loops
        int iteration = 0;

        while (iteration < maxIterations) {
            iteration++;
            logger.info("Processing iteration {} for {}", iteration, currentFilename);

            // Step 3.1. Analyze photo quality and decide on operation
            String qualityAssessment = analyzePhotoQuality(currentUrl);
            String operation = decideOperation(qualityAssessment);

            if (operation.equals("DONE")) {
                logger.info("Photo {} is ready after {} iterations", currentFilename, iteration);
                return currentUrl;
            }

            if (operation.equals("SKIP")) {
                logger.info("Photo {} skipped - not suitable for processing", currentFilename);
                return null;
            }

            // Step 3.2. Send operation command to automation system
            String command = operation + " " + currentFilename;
            PhotoProcessingResponseDto response = sendProcessingCommand(command);

            // Step 3.3. Extract new filename from response
            String newFilename = extractNewFilename(response.getMessage(), currentFilename);
            if (newFilename != null && !newFilename.equals(currentFilename)) {
                currentFilename = newFilename;
                currentUrl = buildNewUrl(currentUrl, newFilename);
                logger.info("Updated to new file: {} -> {}", newFilename, currentUrl);
            } else {
                // If no new file was created, try a different operation or stop
                logger.warn("No improvement for {}, trying different approach or stopping", currentFilename);
                break;
            }
        }

        logger.info("Finished processing {} after {} iterations", currentFilename, iteration);
        return currentUrl;
    }

    private String analyzePhotoQuality(String photoUrl) {
        try {
            String analysisPrompt = promptService.w04d01_createPhotoQualityAnalysisPrompt();

            OpenAiImagePromptRequestDto visionRequest = createVisionRequest(photoUrl, analysisPrompt);
            return openAiAdapter.getAnswerWithImageRequestPayload(visionRequest, "gpt-4.1-mini");

        } catch (Exception e) {
            logger.error("Error analyzing photo quality for {}: {}", photoUrl, e.getMessage(), e);
            return "Error analyzing photo quality. Skipping processing.";
        }
    }

    private String decideOperation(String qualityAssessment) {
        String decisionPrompt = promptService.w04d01_createPhotoOperationDecisionPrompt(qualityAssessment);

        String decision = openAiAdapter.getAnswer(decisionPrompt);

        // Extract operation from decision
        if (decision.contains("REPAIR"))
            return "REPAIR";
        if (decision.contains("BRIGHTEN"))
            return "BRIGHTEN";
        if (decision.contains("DARKEN"))
            return "DARKEN";
        if (decision.contains("DONE"))
            return "DONE";
        if (decision.contains("SKIP"))
            return "SKIP";

        // Default to DONE if unclear
        return "DONE";
    }

    private PhotoProcessingResponseDto sendProcessingCommand(String command) {
        ReportRequestDto request = new ReportRequestDto("photos", apiKey, command);
        return apiExplorerService.postJsonForObject(reportUrl, request, PhotoProcessingResponseDto.class);
    }

    private String extractNewFilename(String message, String originalFilename) {
        // Look for patterns like IMG_123_FXER.PNG or similar processed filenames
        Pattern filenamePattern = Pattern.compile("([A-Za-z0-9_]+\\.(png|jpg|jpeg|PNG|JPG|JPEG))",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = filenamePattern.matcher(message);

        while (matcher.find()) {
            String foundFilename = matcher.group(1);
            // Return the first filename that's different from the original
            if (!foundFilename.equals(originalFilename)) {
                return foundFilename;
            }
        }

        return null; // No new filename found
    }

    private String buildNewUrl(String originalUrl, String newFilename) {
        int lastSlash = originalUrl.lastIndexOf('/');
        return originalUrl.substring(0, lastSlash + 1) + newFilename;
    }

    private String createBarbaraDescription(List<String> photoUrls) {
        if (photoUrls.isEmpty()) {
            throw new RuntimeException("No processed photos available for creating Barbara's description");
        }

        try {
            StringBuilder allAnalyses = new StringBuilder();

            for (int i = 0; i < photoUrls.size(); i++) {
                String photoUrl = photoUrls.get(i);
                String photoAnalysisPrompt = promptService.w04d01_createSinglePhotoBarbaraAnalysisPrompt(i + 1);

                OpenAiImagePromptRequestDto visionRequest = createVisionRequest(photoUrl, photoAnalysisPrompt);
                String analysis = openAiAdapter.getAnswerWithImageRequestPayload(visionRequest, "gpt-4.1");

                allAnalyses.append("Zdjęcie ").append(i + 1).append(":\n").append(analysis).append("\n\n");
            }

            // Create final description based on all analyses
            String finalDescriptionPrompt = promptService
                    .w04d01_createBarbaraFinalDescriptionPrompt(allAnalyses.toString());

            return openAiAdapter.getAnswer(finalDescriptionPrompt);
        } catch (Exception e) {
            logger.error("Error creating Barbara's description: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Barbara's description", e);
        }
    }

    private OpenAiImagePromptRequestDto createVisionRequest(String imageUrl, String prompt) {
        OpenAiImagePromptRequestDto request = new OpenAiImagePromptRequestDto();
        request.setModel("gpt-4.1-mini");

        // Create message with image
        OpenAiImagePromptRequestDto.ImagePromptInput message = new OpenAiImagePromptRequestDto.ImagePromptInput();
        message.setRole("user");

        // Create content with text and image
        List<OpenAiImagePromptRequestDto.ContentPart> contents = new ArrayList<>();

        // Text content
        OpenAiImagePromptRequestDto.ContentPart textContent = new OpenAiImagePromptRequestDto.ContentPart();
        textContent.setType("text");
        textContent.setText(prompt);
        contents.add(textContent);

        // Image content
        OpenAiImagePromptRequestDto.ContentPart imageContent = new OpenAiImagePromptRequestDto.ContentPart();
        imageContent.setType("image_url");
        OpenAiImagePromptRequestDto.ImageUrl imageUrl_obj = new OpenAiImagePromptRequestDto.ImageUrl();
        imageUrl_obj.setUrl(imageUrl);
        imageContent.setImage_url(imageUrl_obj);
        contents.add(imageContent);

        message.setContent(contents);
        request.setMessages(List.of(message));

        return request;
    }

    private PhotoProcessingResponseDto sendFinalReport(String barbaraDescription) {
        ReportRequestDto request = new ReportRequestDto("photos", apiKey, barbaraDescription);
        return apiExplorerService.postJsonForObject(reportUrl, request, PhotoProcessingResponseDto.class);
    }
}