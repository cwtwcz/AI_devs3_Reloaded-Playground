package pl.cwtwcz.service.weeks.week2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.cwtwcz.adapter.LlmAdapter;
import pl.cwtwcz.dto.common.ReportRequestDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.utils.FileUtils;

import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;

@Service
public class W02D01Service {
    private static final Logger logger = LoggerFactory.getLogger(W02D01Service.class);

    private final LlmAdapter groqAdapter;
    private final LlmAdapter openAiAdapter;
    private final PromptService promptService;
    private final ApiExplorerService apiExplorerService;
    private final FlagService flagService;
    private final String aidevsApiKey;
    private final String reportUrl;
    private final String inputDir;

    public W02D01Service(
            @Qualifier("groqAdapter") LlmAdapter groqAdapter, // Should be GroqAdapter bean
            @Qualifier("openAiAdapter") LlmAdapter openAiAdapter, // Should be OpenAiAdapter bean
            PromptService promptService,
            ApiExplorerService apiExplorerService,
            FlagService flagService,
            @Value("${aidevs.api.key}") String aidevsApiKey,
            @Value("${custom.report.url}") String reportUrl,
            @Value("${custom.w02d01.input-dir}") String inputDir
    ) {
        this.groqAdapter = groqAdapter;
        this.openAiAdapter = openAiAdapter;
        this.promptService = promptService;
        this.apiExplorerService = apiExplorerService;
        this.flagService = flagService;
        this.aidevsApiKey = aidevsApiKey;
        this.reportUrl = reportUrl;
        this.inputDir = inputDir;
    }

    public String w02d01() {
        try {
            // Step 1: Transcribe all .m4a files and store in memory
            List<File> audioFiles = FileUtils.listFilesByExtension(inputDir, ".m4a");
            if (audioFiles.isEmpty()) throw new RuntimeException("No audio files found in " + inputDir);
            List<String> transcriptions = audioFiles.stream()
                .peek(audio -> logger.info("Transcribing {}", audio.getName()))
                .map(audio -> groqAdapter.speechToText(audio.getAbsolutePath()))
                .collect(Collectors.toList());

            // Step 2: Concatenate all transcriptions
            String fullContext = String.join("\n", transcriptions);

            // Step 3: Extract street name using LLM
            String llmPrompt = promptService.w02d01_createStreetNamePrompt(fullContext);
            String streetName = openAiAdapter.getAnswer(llmPrompt, "gpt-4.1").trim();
            logger.info("Extracted street name: {}", streetName);

            // Step 4: Send report
            ReportRequestDto reportDto = new ReportRequestDto("mp3", aidevsApiKey, streetName);
            String response = apiExplorerService.postJsonForObject(reportUrl, reportDto, String.class);
            logger.info("Report response: {}", response);

            // Step 5: Extract and return flag
            Optional<String> flag = flagService.findFlagInText(response);
            return flag.orElseThrow(() -> new RuntimeException("No flag found in response."));
        } catch (Exception e) {
            logger.error("Error in w02d01: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}