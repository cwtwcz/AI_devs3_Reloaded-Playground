package pl.cwtwcz.service.weeks.week2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.common.DallEImageResponseDto;
import pl.cwtwcz.dto.common.ReportRequestDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FlagService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class W02D03Service {

    private static final Logger logger = LoggerFactory.getLogger(W02D03Service.class);

    @Value("${aidevs.api.key}")
    private String aidevsApiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${custom.robotid.url.template}")
    private String robotDescriptionUrlTemplate;

    private final OpenAiAdapter llmAdapter;
    private final PromptService promptService;
    private final ApiExplorerService apiExplorerService;
    private final FlagService flagService;

    public String w02d03() {
        // Step 1: Download the robot description from the URL and save in variable
        String url = robotDescriptionUrlTemplate.replace("%aidevs.api.key%", aidevsApiKey);
        String robotDescription = apiExplorerService.postJsonForObject(url, null, String.class);
        logger.info("Robot description: {}", robotDescription);

        // Step 2: Generate a prompt for image generation using PromptService
        String prompt = promptService.w02d03_createImagePromptForRobotDescription(robotDescription);
        String imagePrompt = llmAdapter.getAnswer(prompt);
        logger.info("Image prompt: {}", imagePrompt);

        // Step 3: Generate image using OpenAI DALL-E 3
        DallEImageResponseDto imageResponse = llmAdapter.generateImage(imagePrompt, "dall-e-3");
        String imageUrl = (imageResponse != null && imageResponse.getData() != null
                && !imageResponse.getData().isEmpty())
                        ? imageResponse.getData().get(0).getUrl()
                        : null;
        logger.info("Image URL: {}", imageUrl);

        // Step 4: Send the image URL to the report endpoint
        ReportRequestDto reportRequest = new ReportRequestDto("robotid", aidevsApiKey, imageUrl);
        String reportResponse = apiExplorerService.postJsonForObject(reportUrl, reportRequest, String.class);
        logger.info("Report response: {}", reportResponse);

        // Step 5:Check for flag in response
        return flagService.findFlagInText(reportResponse)
                .orElseThrow(() -> new RuntimeException("No flag found in the response from the report API."));
    }
}