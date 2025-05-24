package pl.cwtwcz.service.weeks.week2;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import pl.cwtwcz.adapter.GroqAdapter;
import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.utils.FileUtils;
import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import pl.cwtwcz.dto.common.ReportW02D04RequestDto;
import pl.cwtwcz.dto.common.ReportW02D04RequestDto.Answer;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;

@RequiredArgsConstructor
@Service
public class W02D04Service {

    private static final Logger logger = LoggerFactory.getLogger(W02D04Service.class);

    private final GroqAdapter groqAdapter;
    private final OpenAiAdapter openAiAdapter;
    private final FileService fileService;
    private final PromptService promptService;
    private final ApiExplorerService apiExplorerService;
    private final FlagService flagService;

    @Value("${custom.w02d04.input-dir}")
    private String inputDir;

    @Value("${aidevs.api.key}")
    private String aidevsApiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    public String w02d04() {
        Map<String, String> transcriptions = new HashMap<>();

        // Step 1: Process mp3 files
        processMp3Files(inputDir, transcriptions);

        // Step 2: Process png files
        processPngFiles(inputDir, transcriptions);

        // Step 3: Load all .txt files (including those generated from mp3/png)
        List<File> txtFiles = FileUtils.listFilesByExtension(inputDir, ".txt");
        for (File txtFile : txtFiles) {
            try {
                String content = java.nio.file.Files.readString(txtFile.toPath());
                transcriptions.put(txtFile.getName(), content);
            } catch (Exception e) {
                continue;
            }
        }

        // Step 4: Extract relevant file names
        String prompt = promptService.extractRelevantNoteFilenamesPrompt(transcriptions);
        logger.info("Prompt: {}", prompt);
        String llmResponse = openAiAdapter.getAnswer(prompt);
        logger.info("LLM response: {}", llmResponse);
        List<String> relevantFileNames = Arrays.asList(llmResponse.split("\\r?\\n"));
        logger.info("Relevant file names: {}", relevantFileNames);

        // Step 5: Send to report endpoint
        ReportW02D04RequestDto reportRequest = new ReportW02D04RequestDto(
                "kategorie", aidevsApiKey, new Answer(new ArrayList<>(), new ArrayList<>()));
        parseRelevantFilesIntoDto(relevantFileNames, reportRequest);
        String reportResponse = apiExplorerService.postJsonForObject(reportUrl, reportRequest, String.class);
        logger.info("Report response: {}", reportResponse);

        // Step 6: Check for flag in response
        return flagService.findFlagInText(reportResponse)
                .orElseThrow(() -> new RuntimeException("No flag found in the response from the report API."));
    }

    private void parseRelevantFilesIntoDto(List<String> relevantFileNames, ReportW02D04RequestDto reportRequest) {
        for (String line : relevantFileNames) {
            String l = line.trim();
            if (l.endsWith("|people")) {
                reportRequest.getAnswer().getPeople().add(l.replace("|people", "").trim());
            } else if (l.endsWith("|hardware")) {
                reportRequest.getAnswer().getHardware().add(l.replace("|hardware", "").trim());
            }
        }
    }

    private void processMp3Files(String inputDir, Map<String, String> transcriptions) {
        List<File> mp3Files = FileUtils.listFilesByExtension(inputDir, ".mp3");
        File processedDir = new File(inputDir, "processed_transcriptions");
        if (!processedDir.exists())
            processedDir.mkdirs();
        for (File mp3File : mp3Files) {
            try {
                String txtFileName = mp3File.getName().replaceAll("\\.mp3$", ".txt");

                File existingTranscription = new File(processedDir, txtFileName);
                if (existingTranscription.exists()) {
                    // caching
                    String existingContent = Files.readString(existingTranscription.toPath());
                    transcriptions.put(mp3File.getName(), existingContent);
                    continue;
                } else {
                    String transcription = groqAdapter.speechToText(mp3File.getAbsolutePath(), "EN");
                    File txtFile = new File(processedDir, txtFileName);
                    Files.writeString(txtFile.toPath(), transcription);
                    transcriptions.put(mp3File.getName(), transcription);
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    private void processPngFiles(String inputDir, Map<String, String> transcriptions) {
        List<File> pngFiles = FileUtils.listFilesByExtension(inputDir, ".png");
        File processedDir = new File(inputDir, "processed_transcriptions");
        if (!processedDir.exists())
            processedDir.mkdirs();
        for (File pngFile : pngFiles) {
            try {
                String txtFileName = pngFile.getName().replaceAll("\\.png$", ".txt");

                File existingTranscription = new File(processedDir, txtFileName);
                if (existingTranscription.exists()) {
                    // caching
                    String existingContent = Files.readString(existingTranscription.toPath());
                    transcriptions.put(pngFile.getName(), existingContent);
                    continue;
                } else {
                    String base64Image = fileService.readFileAsBase64(pngFile.getAbsolutePath());
                    String prompt = promptService.extractTextFromImagePrompt();
                    OpenAiImagePromptRequestDto dto = OpenAiImagePromptRequestDto.build("gpt-4o", prompt, base64Image);
                    String extractedText = openAiAdapter.getAnswerWithImageRequestPayload(dto, "gpt-4o");
                    File txtFile = new File(processedDir, txtFileName);
                    Files.writeString(txtFile.toPath(), extractedText);
                }
            } catch (Exception e) {
                continue;
            }
        }
    }
}