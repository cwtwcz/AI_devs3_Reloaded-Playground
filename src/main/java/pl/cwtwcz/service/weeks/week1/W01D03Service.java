package pl.cwtwcz.service.weeks.week1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import pl.cwtwcz.adapter.LlmAdapter;
import pl.cwtwcz.dto.week1.day3.AnswerDataRequestDto;
import pl.cwtwcz.dto.week1.day3.InputDataDto;
import pl.cwtwcz.dto.week1.day3.AnswerDto;
import pl.cwtwcz.dto.week1.day3.ResponseTestDataItemDto;
import pl.cwtwcz.dto.week1.day3.ResponseTestDetailDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PromptService;

import static java.lang.Integer.parseInt;
import static pl.cwtwcz.utils.StringUtils.isEmpty;
import static pl.cwtwcz.utils.StringUtils.isNotEmpty;

import java.util.List;
import java.util.Optional;

@Service
public class W01D03Service {
    private static final Logger logger = LoggerFactory.getLogger(W01D03Service.class);

    private final LlmAdapter llmAdapter;
    private final PromptService promptService;
    private final FlagService flagService;
    private final ApiExplorerService apiExplorerService;
    private final FileService fileService;
    private final String aidevsApiKey;
    private final String w01d03InputFilePath;
    private final String reportUrl;

    public W01D03Service(
            LlmAdapter llmAdapter,
            PromptService promptService,
            FlagService flagService,
            ApiExplorerService apiExplorerService,
            FileService fileService,
            @Value("${aidevs.api.key}") String aidevsApiKey,
            @Value("${custom.w01d03.input-file.path}") String w01d03InputFilePath,
            @Value("${custom.report.url}") String reportUrl) {
        this.llmAdapter = llmAdapter;
        this.promptService = promptService;
        this.flagService = flagService;
        this.apiExplorerService = apiExplorerService;
        this.fileService = fileService;
        this.aidevsApiKey = aidevsApiKey;
        this.w01d03InputFilePath = w01d03InputFilePath;
        this.reportUrl = reportUrl;
    }

    public String w01d03() {
        logger.info("Starting w01d03 task.");

        // Step 1: Read input data from file
        InputDataDto inputData = fileService.readJsonFileToObject(this.w01d03InputFilePath, InputDataDto.class);

        // Step 2: Build response data for each test data item
        List<ResponseTestDataItemDto> responseTestDataItems = inputData.getTestData().stream().map(item -> {
            // Step 2.1: Calculate arithmetic sum from expression
            int calculatedAnswer = evaluateArithmeticSumFromExpression(item.getQuestion());

            // Step 2.2: If question is not empty, get answer from LLM
            ResponseTestDetailDto responseTestDetail = null;
            if (item.getTest() != null && isNotEmpty(item.getTest().getQuestion())) {
                String llmSystemPrompt = promptService.w01d03_createShortAnswerPrompt(item.getTest().getQuestion());
                String llmAnswer = llmAdapter.getAnswer(llmSystemPrompt);
                logger.info("LLM Question: {}, LLM Answer: {}", item.getTest().getQuestion(), llmAnswer);
                responseTestDetail = new ResponseTestDetailDto(item.getTest().getQuestion(), llmAnswer.trim());
            }
            return new ResponseTestDataItemDto(item.getQuestion(), calculatedAnswer, responseTestDetail);
        }).toList();

        // Step 3: Build response data
        AnswerDataRequestDto finalPayload = AnswerDataRequestDto.builder()
                .task("JSON")
                .apikey(this.aidevsApiKey)
                .answer(AnswerDto.builder()
                        .apikey(this.aidevsApiKey)
                        .description(inputData.getDescription())
                        .copyright(inputData.getCopyright())
                        .testData(responseTestDataItems)
                        .build())
                .build();

        // Step 4: Send answer to API
        String reportResponse = apiExplorerService.postJsonForObject(this.reportUrl, finalPayload, String.class);
        logger.info("Report sent to {}. Response: {}", this.reportUrl, reportResponse);

        // Step 5: Check if flag is in response
        Optional<String> flag = flagService.findFlagInText(reportResponse);
        if (flag.isPresent()) {
            logger.info("Flag detected in report. Verification process complete.");
            return flag.get();
        }

        logger.info("Finished w01d03 task. Flag not found.");
        return "Flag not found";
    }

    private int evaluateArithmeticSumFromExpression(String expression) {
        if (isEmpty(expression)) {
            throw new RuntimeException("Expression cannot be null or empty.");
        }

        String[] parts = expression.split("\\s*\\+\\s*"); // Split by "+" with optional surrounding spaces
        if (parts.length != 2) {
            throw new RuntimeException("Expression format incorrect. Expected 'number + number'. Got: " + expression);
        }

        try {
            return parseInt(parts[0].trim()) + parseInt(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number format in expression: " + expression, e);
        }
    }
}