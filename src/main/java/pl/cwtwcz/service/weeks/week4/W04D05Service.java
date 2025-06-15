package pl.cwtwcz.service.weeks.week4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.common.OpenAiImagePromptRequestDto;
import pl.cwtwcz.dto.week4.NotesQuestionDto;
import pl.cwtwcz.dto.week4.NotesAnswerDto;
import pl.cwtwcz.dto.week4.CentralaResponseDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.PdfProcessingService;
import pl.cwtwcz.service.DatabaseService;
import pl.cwtwcz.service.DatabaseQueryService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class W04D05Service {

    private static final Logger logger = LoggerFactory.getLogger(W04D05Service.class);

    private static final int MIN_VALID_CONTENT_LENGTH = 100;
    private static final int PDF_TEXT_START_PAGE = 1;
    private static final int PDF_TEXT_END_PAGE = 18;
    private static final int PDF_OCR_PAGE = 19;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${centrala.base.url}")
    private String centralaBaseUrl;

    @Value("${custom.w04d05.database.path}")
    private String dbPath;

    @Value("${custom.w04d05.pdf.path}")
    private String pdfPath;

    private final ApiExplorerService apiExplorerService;
    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;
    private final FlagService flagService;
    private final PdfProcessingService pdfProcessingService;
    private final DatabaseService databaseService;
    private final DatabaseQueryService databaseQueryService;
    private final RestTemplate restTemplate;

    public String w04d05() {
        final int MAX_ATTEMPTS = 30;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                logger.info("Starting W04D05 - Rafał's notebook analysis (Attempt {}/{})", attempt, MAX_ATTEMPTS);

                // Step 1. Initialize database - if needed
                initializeDatabase();

                // Step 2. Fetch and store questions
                List<NotesQuestionDto> questions = fetchAndStoreQuestions();
                logger.info("Loaded {} questions", questions.size());

                // Step 3. Process PDF and store content (with quality validation)
                String fullNotebookContent = processAndStoreNotebookContent();

                // Step 4. Find answers for all questions
                Map<String, String> answers = findAnswersForAllQuestions(questions, fullNotebookContent);

                // Step 5. Submit answers to centrala
                String result = submitAnswers(answers);

                // Step 6. Extract flag if present
                Optional<String> flag = flagService.findFlagInText(result);
                if (flag.isPresent()) {
                    logger.info("Flag found on attempt {}/{}: {}", attempt, MAX_ATTEMPTS, flag.get());
                    return flag.get();
                } else {
                    logger.warn("No flag found on attempt {}/{}. Result: {}", attempt, MAX_ATTEMPTS, result);
                    if (attempt < MAX_ATTEMPTS) {
                        logger.info("Retrying...");
                    } else {
                        logger.error("All {} attempts failed to find flag. Returning last result.", MAX_ATTEMPTS);
                        return result;
                    }
                }
            } catch (Exception e) {
                logger.error("Error in W04D05 task on attempt {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage(), e);
                if (attempt < MAX_ATTEMPTS) {
                    logger.info("Retrying due to error...");
                } else {
                    throw new RuntimeException("Failed to complete W04D05 task after " + MAX_ATTEMPTS + " attempts", e);
                }
            }

            // Wait 30 seconds before retrying. OpenAI API restrictions
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                logger.error("Error in W04D05 task on attempt {}/{}: {}", attempt, MAX_ATTEMPTS, e.getMessage(), e);
                throw new RuntimeException("Failed to complete W04D05 task after " + MAX_ATTEMPTS + " attempts", e);
            }
        }

        // This should never be reached due to the logic above, but keeping for safety
        throw new RuntimeException("Unexpected end of retry loop");
    }

    private void initializeDatabase() {
        logger.info("Ensuring database tables exist (will skip if already created)");
        databaseService.executeDDL(dbPath, databaseQueryService.createQuestionsTable());
        databaseService.executeDDL(dbPath, databaseQueryService.createAnswersTable());
        databaseService.executeDDL(dbPath, databaseQueryService.createNotebookContentTable());
    }

    private List<NotesQuestionDto> fetchAndStoreQuestions() {
        // Check if questions already exist - use cached questions to save API calls
        if (databaseService.hasData(dbPath, "questions")) {
            logger.info("Questions already exist in database - loading from cached data to save API calls");
            return getQuestionsFromDatabase();
        }

        // Step 1. Fetch questions from API (only if not cached)
        String questionsUrl = centralaBaseUrl + "data/" + apiKey + "/notes.json";
        logger.info("No cached questions found - fetching from API: {}", questionsUrl);

        @SuppressWarnings("unchecked")
        Map<String, String> questionsMap = restTemplate.getForObject(questionsUrl, Map.class);
        List<NotesQuestionDto> questions = new ArrayList<>();

        // Step 2. Store questions in database for future use
        for (Map.Entry<String, String> entry : questionsMap.entrySet()) {
            NotesQuestionDto question = new NotesQuestionDto(entry.getKey(), entry.getValue());
            questions.add(question);

            databaseService.executeUpdate(dbPath, databaseQueryService.insertOrReplaceQuestion(),
                    entry.getKey(), entry.getValue());
        }

        logger.info("Fetched and stored {} questions in database for future use", questions.size());
        return questions;
    }

    private List<NotesQuestionDto> getQuestionsFromDatabase() {
        List<Map<String, Object>> rows = databaseService.executeQuery(dbPath,
                databaseQueryService.selectAllQuestions());

        List<NotesQuestionDto> questions = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            questions.add(new NotesQuestionDto(
                    (String) row.get("id"),
                    (String) row.get("question")));
        }
        return questions;
    }

    private String processAndStoreNotebookContent() {
        // Check what content exists and validate each type separately
        boolean needsTextProcessing = true;
        boolean needsOcrProcessing = true;

        if (databaseService.hasData(dbPath, "notebook_content")) {
            logger.info("Notebook content found in database - validating each entry separately");

            // Validate and determine what needs reprocessing
            ContentValidationResult validation = validateAndCleanNotebookContent();
            needsTextProcessing = !validation.hasValidText;
            needsOcrProcessing = !validation.hasValidOcr;

            if (!needsTextProcessing && !needsOcrProcessing) {
                return getFullNotebookContent();
            }
        }

        // Process only what's needed
        if (needsTextProcessing) {
            String textContent = pdfProcessingService.extractTextFromPages(pdfPath, PDF_TEXT_START_PAGE,
                    PDF_TEXT_END_PAGE);
            databaseService.executeUpdate(dbPath, databaseQueryService.insertNotebookContent(),
                    "text", textContent, "pages " + PDF_TEXT_START_PAGE + "-" + PDF_TEXT_END_PAGE);
            logger.info("Stored text content from pages {}-{}", PDF_TEXT_START_PAGE, PDF_TEXT_END_PAGE);
        } else {
            logger.info("Using cached text content from pages 1-18");
        }

        if (needsOcrProcessing) {
            logger.info("Processing OCR content from PDF page {} (consuming tokens)", PDF_OCR_PAGE);

            String base64Image = pdfProcessingService.convertPageToBase64Image(pdfPath, PDF_OCR_PAGE);
            String ocrPrompt = promptService.w04d05_createOcrPrompt(getFullNotebookContent());
            OpenAiImagePromptRequestDto imageRequest = OpenAiImagePromptRequestDto.build(
                    "gpt-4o", ocrPrompt, base64Image);
            String ocrText = openAiAdapter.getAnswerWithImageRequestPayload(imageRequest, "gpt-4o");
            if (ocrText != null && isValidContent(ocrText)) {
                logger.warn("OCR seems to have failed. Response: {}", ocrText);
            }
            databaseService.executeUpdate(dbPath, databaseQueryService.insertNotebookContent(), "ocr", ocrText,
                    "page 19 OCR");
            logger.info("Stored OCR content from page 19");
        } else {
            logger.info("Using cached OCR content from page 19");
        }

        // Step 3. Final validation
        ContentValidationResult finalValidation = validateNotebookContentDetailed();
        if (finalValidation.hasValidText && finalValidation.hasValidOcr) {
            logger.info("Successfully validated all notebook content in database");
            return getFullNotebookContent();
        } else {
            String errorMessage = "Some notebook content still fails validation - Text: " + finalValidation.hasValidText
                    + ", OCR: " + finalValidation.hasValidOcr;
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    private ContentValidationResult validateAndCleanNotebookContent() {
        try {
            List<Map<String, Object>> rows = databaseService.executeQuery(dbPath,
                    databaseQueryService.selectNotebookContent());

            boolean hasValidText = false;
            boolean hasValidOcr = false;

            for (Map<String, Object> row : rows) {
                String contentType = (String) row.get("content_type");
                String content = (String) row.get("content");
                if ("text".equals(contentType)) {
                    if (isValidContent(content)) {
                        hasValidText = true;
                        logger.info("Text content (pages 1-18) validation: PASSED - keeping cached content");
                    } else {
                        logger.warn("Text content (pages 1-18) validation: FAILED - will remove and reprocess");
                        databaseService.executeUpdate(dbPath,
                                "DELETE FROM notebook_content WHERE content_type = 'text'");
                    }
                } else if ("ocr".equals(contentType)) {
                    if (isValidContent(content)) {
                        hasValidOcr = true;
                        logger.info("OCR content (page 19) validation: PASSED - keeping cached content");
                    } else {
                        logger.warn("OCR content (page 19) validation: FAILED - will remove and reprocess");
                        databaseService.executeUpdate(dbPath,
                                "DELETE FROM notebook_content WHERE content_type = 'ocr'");
                    }
                }
            }

            logger.info("Content validation result - Valid text: {}, Valid OCR: {}",
                    hasValidText, hasValidOcr);

            return new ContentValidationResult(hasValidText, hasValidOcr);

        } catch (Exception e) {
            logger.error("Error during content validation and cleanup: {}", e.getMessage(), e);
            return new ContentValidationResult(false, false);
        }
    }

    private ContentValidationResult validateNotebookContentDetailed() {
        try {
            List<Map<String, Object>> rows = databaseService.executeQuery(dbPath,
                    databaseQueryService.selectNotebookContent());

            boolean hasValidText = false;
            boolean hasValidOcr = false;

            for (Map<String, Object> row : rows) {
                String contentType = (String) row.get("content_type");
                String content = (String) row.get("content");

                if ("text".equals(contentType)) {
                    hasValidText = isValidContent(content);
                } else if ("ocr".equals(contentType)) {
                    hasValidOcr = isValidContent(content);
                }
            }

            return new ContentValidationResult(hasValidText, hasValidOcr);

        } catch (Exception e) {
            logger.error("Error during detailed content validation: {}", e.getMessage(), e);
            return new ContentValidationResult(false, false);
        }
    }

    private boolean isValidContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        return content.length() >= MIN_VALID_CONTENT_LENGTH;
    }

    private Map<String, String> findAnswersForAllQuestions(List<NotesQuestionDto> questions,
            String fullNotebookContent) {
        Map<String, String> answers = new HashMap<>();

        for (NotesQuestionDto question : questions) {
            String existingCorrectAnswer = getCorrectAnswerFromDatabase(question.getQuestionId());
            if (existingCorrectAnswer != null) {
                logger.info("Using confirmed correct answer for question {}: {}",
                        question.getQuestionId(), existingCorrectAnswer);
                answers.put(question.getQuestionId(), existingCorrectAnswer);
            } else {
                // Generate new answer only if no confirmed correct answer exists
                String answer = findAnswerForQuestion(question, fullNotebookContent);
                answers.put(question.getQuestionId(), answer);
                logger.info("Question {} (NEW/RETRY): {}", question.getQuestionId(), answer);
            }
        }

        return answers;
    }

    private String findAnswerForQuestion(NotesQuestionDto question, String fullNotebookContent) {
        // Check if we already have a correct answer
        String existingAnswer = getCorrectAnswerFromDatabase(question.getQuestionId());
        if (existingAnswer != null) {
            logger.info("Found existing correct answer for question {}: {}",
                    question.getQuestionId(), existingAnswer);
            return existingAnswer;
        }

        // Get previous incorrect answers and all hints
        Set<String> previousIncorrectAnswers = getIncorrectAnswersFromDatabase(question.getQuestionId());
        Set<String> allHints = getAllHintsFromDatabase(question.getQuestionId());
        String prompt = promptService.w04d05_createQuestionAnswerPrompt(
                question.getQuestion(), fullNotebookContent, previousIncorrectAnswers, String.join("; ", allHints));

        return openAiAdapter.getAnswer(prompt, "gpt-4o");
    }

    private String getFullNotebookContent() {
        List<Map<String, Object>> rows = databaseService.executeQuery(dbPath,
                databaseQueryService.selectNotebookContent());

        StringBuilder content = new StringBuilder();
        for (Map<String, Object> row : rows) {
            content.append("=== ").append(row.get("source_info")).append(" ===\n");
            content.append(row.get("content")).append("\n\n");
        }
        return content.toString();
    }

    private String getCorrectAnswerFromDatabase(String questionId) {
        Object result = databaseService.executeSingleValue(dbPath,
                databaseQueryService.selectCorrectAnswer(), questionId);
        return result != null ? (String) result : null;
    }

    private Set<String> getIncorrectAnswersFromDatabase(String questionId) {
        List<Map<String, Object>> rows = databaseService.executeQuery(dbPath,
                databaseQueryService.selectIncorrectAnswers(), questionId);

        return rows.stream()
                .map(row -> (String) row.get("answer"))
                .filter(answer -> answer != null && !answer.trim().isEmpty())
                .collect(Collectors.toSet());
    }

    private Set<String> getAllHintsFromDatabase(String questionId) {
        List<Map<String, Object>> rows = databaseService.executeQuery(dbPath,
                databaseQueryService.selectAllHints(), questionId);

        return rows.stream()
                .map(row -> (String) row.get("hint"))
                .filter(hint -> hint != null && !hint.trim().isEmpty())
                .collect(Collectors.toSet());
    }

    private void storeAnswer(String questionId, String answer, boolean isCorrect, String hint) {
        databaseService.executeUpdate(dbPath, databaseQueryService.insertAnswer(),
                questionId, answer, isCorrect, hint);
    }

    private String submitAnswers(Map<String, String> answers) {
        NotesAnswerDto answerDto = new NotesAnswerDto("notes", apiKey, answers);

        try {
            CentralaResponseDto response = apiExplorerService.postJsonForObject(
                    reportUrl, answerDto, CentralaResponseDto.class);

            logger.info("Centrala response: {}", response);
            processSubmissionResponse(response, answers);
            return response.toString();
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            logger.info("Centrala returned error: {}", errorMessage);
            processErrorResponse(errorMessage, answers);
            return errorMessage;
        }
    }

    private void processSubmissionResponse(CentralaResponseDto response, Map<String, String> submittedAnswers) {
        logger.info("Processing submission response - isSuccess: {}", response.isSuccess());

        if (response.isSuccess()) {
            logger.info("Success response - processing {} submitted answers", submittedAnswers.size());

            for (Map.Entry<String, String> entry : submittedAnswers.entrySet()) {
                String questionId = entry.getKey();
                String answer = entry.getValue();

                String existingCorrectAnswer = getCorrectAnswerFromDatabase(questionId);
                logger.info("Question {}: submitted='{}', existing='{}'", questionId, answer, existingCorrectAnswer);

                if (existingCorrectAnswer == null || !existingCorrectAnswer.equals(answer)) {
                    storeAnswer(questionId, answer, true, null);
                    logger.info("STORED: Marked answer for question {} as CORRECT: {}", questionId, answer);
                } else {
                    logger.info("SKIPPED: Answer for question {} already exists as correct: {}", questionId,
                            existingCorrectAnswer);
                }
            }
            logger.info("Finished processing success response");
        } else {
            String failedQuestionInfo = response.getMessage();
            String hint = response.getHint();

            if (failedQuestionInfo != null && failedQuestionInfo.contains("Answer for question")) {
                processPartialFeedback(failedQuestionInfo, hint, submittedAnswers);
            }
        }
    }

    private void processErrorResponse(String errorMessage, Map<String, String> submittedAnswers) {
        try {
            String jsonPart = extractJsonFromErrorMessage(errorMessage);
            if (jsonPart != null) {
                CentralaResponseDto errorResponse = restTemplate.getForObject(
                        "data:application/json," + jsonPart, CentralaResponseDto.class);

                if (errorResponse != null && errorResponse.getMessage() != null &&
                        errorResponse.getMessage().contains("Answer for question")) {
                    processPartialFeedback(errorResponse.getMessage(), errorResponse.getHint(), submittedAnswers);
                }
            }
        } catch (Exception e) {
            processErrorResponseFallback(errorMessage, submittedAnswers);
        }
    }

    private void processErrorResponseFallback(String errorMessage, Map<String, String> submittedAnswers) {
        if (errorMessage.contains("Answer for question")) {
            String questionNumber = null;
            String hint = null;

            // Extract question number using regex
            Pattern questionPattern = Pattern.compile("question (\\d+)");
            Matcher questionMatcher = questionPattern.matcher(errorMessage);
            if (questionMatcher.find()) {
                questionNumber = questionMatcher.group(1);
                // Pad with zero if single digit (01, 02, etc.)
                if (questionNumber.length() == 1) {
                    questionNumber = "0" + questionNumber;
                }
            }

            // Extract hint using regex
            Pattern hintPattern = Pattern.compile("\"hint\"\\s*:\\s*\"([^\"]+)\"");
            Matcher hintMatcher = hintPattern.matcher(errorMessage);
            if (hintMatcher.find()) {
                hint = hintMatcher.group(1);
                // Handle unicode escapes
                hint = unescapeUnicode(hint);
            }

            if (questionNumber != null) {
                String failedQuestionInfo = "Answer for question " + questionNumber + " is incorrect";
                processPartialFeedback(failedQuestionInfo, hint, submittedAnswers);
            }
        }
    }

    private String unescapeUnicode(String input) {
        if (input == null)
            return null;

        Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = pattern.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            int codePoint = Integer.parseInt(hexCode, 16);
            matcher.appendReplacement(result, String.valueOf((char) codePoint));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String extractJsonFromErrorMessage(String errorMessage) {
        int jsonStart = errorMessage.indexOf("{");
        int jsonEnd = errorMessage.lastIndexOf("}");

        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            return errorMessage.substring(jsonStart, jsonEnd + 1);
        }
        return null;
    }

    private void processPartialFeedback(String failedQuestionInfo, String hint, Map<String, String> submittedAnswers) {
        String failedQuestionId = null;
        if (failedQuestionInfo.contains("question ")) {
            String[] parts = failedQuestionInfo.split("question ");
            if (parts.length > 1) {
                String questionPart = parts[1].split(" ")[0];
                failedQuestionId = questionPart;
            }
        }

        if (failedQuestionId != null) {
            List<String> orderedQuestionIds = new ArrayList<>(submittedAnswers.keySet());
            Collections.sort(orderedQuestionIds);
            int failedIndex = orderedQuestionIds.indexOf(failedQuestionId);

            if (failedIndex != -1) {
                for (int i = 0; i < failedIndex; i++) {
                    String questionId = orderedQuestionIds.get(i);
                    String answer = submittedAnswers.get(questionId);

                    String existingCorrectAnswer = getCorrectAnswerFromDatabase(questionId);
                    if (existingCorrectAnswer == null || !existingCorrectAnswer.equals(answer)) {
                        storeAnswer(questionId, answer, true, null);
                        logger.info("Marked answer for question {} as CORRECT: {}", questionId, answer);
                    }
                }

                // Mark the failed answer as incorrect with hint (or NULL)
                String failedAnswer = submittedAnswers.get(failedQuestionId);
                String hintToSave = (hint != null && !hint.trim().isEmpty()) ? hint.trim() : null;
                storeAnswer(failedQuestionId, failedAnswer, false, hintToSave);
                logger.info("Marked answer for question {} as INCORRECT with hint: {}", failedQuestionId, hintToSave);
            }
        }
    }

    @AllArgsConstructor
    private static class ContentValidationResult {
        final boolean hasValidText;
        final boolean hasValidOcr;
    }
}