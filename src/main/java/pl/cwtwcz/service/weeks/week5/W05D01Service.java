package pl.cwtwcz.service.weeks.week5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.week4.CentralaResponseDto;
import pl.cwtwcz.dto.week5.PhoneAnswersDto;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.DatabaseQueryService;
import pl.cwtwcz.service.DatabaseService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.PromptService;

import java.io.File;
import java.util.*;

@RequiredArgsConstructor
@Service
public class W05D01Service {

    private static final Logger logger = LoggerFactory.getLogger(W05D01Service.class);

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.w05d01.phone.data.path}")
    private String phoneDataPath;

    @Value("${custom.w05d01.phone.questions.path}")
    private String phoneQuestionsPath;

    @Value("${custom.w05d01.facts.dir}")
    private String factsDir;

    @Value("${custom.w05d01.database.path}")
    private String dbPath;

    private final ApiExplorerService apiExplorerService;
    private final OpenAiAdapter openAiAdapter;
    private final FlagService flagService;
    private final FileService fileService;
    private final DatabaseService databaseService;
    private final DatabaseQueryService databaseQueryService;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;
    private final W05D01ConversationReconstructionService conversationReconstructionService;

    public String analyzeConversationsStep() {
        try {
            logger.info("Starting W05D01 - Conversations analysis step");

            // Step 1. Ensure database is initialized
            ensureDatabaseInitialized();

            // Step 2. Check if conversations are reconstructed
            if (!databaseService.hasData(dbPath, "phone_conversations")) {
                return "ERROR: No reconstructed conversations found. Please run reconstruction step first.";
            }

            // Step 3. Load reconstructed conversations from database
            List<Map<String, Object>> conversations = conversationReconstructionService.getCachedConversations();
            if (conversations.isEmpty()) {
                return "ERROR: Could not load reconstructed conversations from database.";
            }

            logger.info("Loaded {} conversations from database", conversations.size());

            // Step 4. Load questions and facts
            Map<String, String> questions = loadQuestions();
            Map<String, String> facts = loadFacts();

            // Step 5. Answer questions and validate with feedback loop
            String result = answerQuestionsAndValidate(questions, conversations, facts);

            // Step 6. Extract flag if present
            Optional<String> flag = flagService.findFlagInText(result);
            if (flag.isPresent()) {
                logger.info("Flag found: {}", flag.get());
                return flag.get();
            } else {
                logger.warn("No flag found. Result: {}", result);
                return result;
            }
        } catch (Exception e) {
            logger.error("Error processing W05D01 analysis step: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process W05D01 analysis step", e);
        }
    }

    private Map<String, String> loadQuestions() {
        try {
            String jsonContent = fileService.readStringFromFile(phoneQuestionsPath);
            Map<String, Object> rawQuestions = objectMapper.readValue(jsonContent,
                    new TypeReference<Map<String, Object>>() {
                    });

            Map<String, String> questions = new HashMap<>();
            for (Map.Entry<String, Object> entry : rawQuestions.entrySet()) {
                questions.put(entry.getKey(), (String) entry.getValue());
            }

            logger.info("Loaded {} questions", questions.size());
            return questions;

        } catch (Exception e) {
            logger.error("Error loading questions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load questions", e);
        }
    }

    private Map<String, String> loadFacts() {
        try {
            Map<String, String> facts = new HashMap<>();
            File factsDirectory = new File(factsDir);

            if (factsDirectory.exists() && factsDirectory.isDirectory()) {
                File[] factFiles = factsDirectory.listFiles((dir, name) -> name.endsWith(".txt"));

                if (factFiles != null) {
                    for (File factFile : factFiles) {
                        String content = fileService.readStringFromFile(factFile.getAbsolutePath());
                        facts.put(factFile.getName(), content);
                    }
                }
            }

            logger.info("Loaded {} fact files", facts.size());
            return facts;

        } catch (Exception e) {
            logger.error("Error loading facts: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load facts", e);
        }
    }

    private String answerQuestionsAndValidate(Map<String, String> questions,
            List<Map<String, Object>> conversations,
            Map<String, String> facts) {
        logger.info("Starting integrated answer collection and validation process for {} questions", questions.size());

        int maxAttempts = 10;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            logger.info("Attempt {}/{} - Collecting answers", attempt, maxAttempts);

            // Step 1: Collect answers (from cache or LLM with feedback)
            Map<String, String> currentAnswers = collectAnswers(questions, conversations, facts);

            // Step 2: Validate answers through submission
            logger.info("Submitting answers for validation - attempt {}", attempt);
            String result = submitAnswers(currentAnswers);

            // Step 3: Check if all answers are correct
            if (!result.contains("incorrect") && !result.contains("too long")) {
                logger.info("All answers accepted on attempt {}", attempt);
                // Mark all submitted answers as correct
                markAnswersAsCorrect(currentAnswers);
                return result;
            }

            // Step 4: Process validation feedback
            String problemQuestion = extractProblemQuestionFromError(result);
            if (problemQuestion != null) {
                logger.info("Question {} needs improvement on attempt {}", problemQuestion, attempt);

                // Store the incorrect answer for future feedback (except API endpoints)
                String problemQuestionText = questions.get(problemQuestion);
                if (problemQuestionText == null || !problemQuestionText.contains("endpoint API")) {
                    storeIncorrectAnswer(problemQuestion, currentAnswers.get(problemQuestion), result, attempt);
                } else {
                    logger.info(
                            "Skipping incorrect answer storage for API endpoint question {} - answer changes every minute",
                            problemQuestion);
                }

                // Mark all questions before the problematic one as correct
                markPreviousAnswersAsCorrect(problemQuestion, currentAnswers);
            } else {
                logger.warn("Could not identify problematic question from error: {}", result);
                break;
            }

            // Sleep between attempts
            try {
                Thread.sleep(20000);
            } catch (InterruptedException e) {
                logger.error("Thread interrupted: {}", e.getMessage());
                throw new RuntimeException("Thread interrupted", e);
            }
        }

        logger.warn("Max validation attempts reached without success");
        return "Max attempts reached: " + maxAttempts;
    }

    private Map<String, String> collectAnswers(Map<String, String> questions,
            List<Map<String, Object>> conversations,
            Map<String, String> facts) {
        logger.info("Collecting answers for {} questions", questions.size());

        Map<String, String> answers = new HashMap<>();

        for (Map.Entry<String, String> entry : questions.entrySet()) {
            String questionId = entry.getKey();
            String question = entry.getValue();

            // Step 1: Check if correct answer already exists in database
            // Exception: Don't cache API endpoint answers as they change every minute
            String existingAnswer = null;
            if (!question.contains("endpoint API")) {
                existingAnswer = getCachedAnswer(questionId);
                if (existingAnswer != null) {
                    logger.info("Using cached correct answer for question {}: {}", questionId, existingAnswer);
                    answers.put(questionId, existingAnswer);
                    continue;
                }
            } else {
                logger.info("Skipping cache for API endpoint question {} - answer changes every minute", questionId);
            }

            // Step 2: Generate answer with LLM (including feedback from incorrect history)
            String answer = generateAnswerWithFeedback(question, questionId, conversations, facts);

            // Step 3: Clean API responses if needed
            if (question.contains("endpoint API") && answer.contains("Error calling API:")) {
                answer = extractCleanApiResponse(answer);
            }

            logger.info("Generated new answer for question {}: {}", questionId, answer);
            answers.put(questionId, answer);
        }

        return answers;
    }

    private String getCachedAnswer(String questionId) {
        Object result = databaseService.executeSingleValue(dbPath,
                databaseQueryService.selectCorrectPhoneAnswer(), questionId);
        return result != null ? (String) result : null;
    }

    private String handleApiEndpointQuestion(List<Map<String, Object>> conversations) {
        // Get correct API endpoint from question 02 answer
        String endpoint = getCachedAnswer("02");
        if (endpoint == null) {
            logger.warn("No API endpoint found in question 02 answer");
            return "No endpoint found";
        }

        // Extract password from conversations
        String password = extractPassword(conversations);
        if (password == null) {
            logger.warn("No password found in conversations");
            return "No password found";
        }

        // Call the API endpoint
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("password", password);

            String response = apiExplorerService.postJsonForString(endpoint, payload);
            logger.info("API endpoint response: {}", response);

            // Extract message field from JSON response
            String messageValue = extractMessageFromJson(response);
            if (messageValue != null) {
                logger.info("Extracted message from API response: {}", messageValue);
                return messageValue;
            } else {
                logger.warn("Could not extract message from API response, returning full response");
                return response;
            }

        } catch (Exception e) {
            logger.error("Error calling API endpoint: {}", e.getMessage(), e);
            String errorMessage = "Error calling API: " + e.getMessage();
            // Extract clean message from error for storage
            return extractCleanApiResponse(errorMessage);
        }
    }

    private String extractPassword(List<Map<String, Object>> conversations) {
        // Use LLM to extract password from conversations
        String prompt = promptService.w05d01_createPasswordExtractionPrompt(conversations);
        String result = openAiAdapter.getAnswer(prompt, "gpt-4o");

        // Clean the result - remove any extra text, keep only the password
        String cleanedPassword = result.trim();
        if (cleanedPassword.toLowerCase().startsWith("hasło:")
                || cleanedPassword.toLowerCase().startsWith("password:")) {
            cleanedPassword = cleanedPassword.split(":", 2)[1].trim();
        }

        // Remove quotes if present
        cleanedPassword = cleanedPassword.replaceAll("^[\"']|[\"']$", "");

        if (cleanedPassword.isEmpty()) {
            logger.warn("No password found in conversations");
            return null;
        }

        logger.info("Extracted password: {}", cleanedPassword);
        return cleanedPassword;
    }

    private String submitAnswers(Map<String, String> answers) {
        PhoneAnswersDto answerDto = new PhoneAnswersDto("phone", apiKey, answers);

        try {
            logger.info("Submitting answers to centrala: {}", answers);
            CentralaResponseDto response = apiExplorerService.postJsonForObject(
                    reportUrl, answerDto, CentralaResponseDto.class);

            logger.info("Centrala response: {}", response);

            return response.toString();

        } catch (RuntimeException e) {
            logger.info("Centrala returned error: {}", e.getMessage());
            return e.getMessage();
        }
    }

    private String extractProblemQuestionFromError(String errorMessage) {
        // Parse error message like: "Answer for question 01 is incorrect"
        if (errorMessage.contains("Answer for question")) {
            try {
                String[] parts = errorMessage.split("question ");
                if (parts.length > 1) {
                    String questionPart = parts[1].split(" ")[0];
                    return questionPart; // e.g., "01"
                }
            } catch (Exception e) {
                logger.warn("Error parsing question number from error message: {}", e.getMessage());
            }
        }
        // Parse error message like: "Answer for question 01 is too long"
        if (errorMessage.contains("too long")) {
            try {
                String[] parts = errorMessage.split("question ");
                if (parts.length > 1) {
                    String questionPart = parts[1].split(" ")[0];
                    return questionPart;
                }
            } catch (Exception e) {
                logger.warn("Error parsing question number from length error: {}", e.getMessage());
            }
        }
        return null;
    }

    private void markAnswersAsCorrect(Map<String, String> answers) {
        // Load questions to check which ones are API endpoints
        Map<String, String> questions = loadQuestions();

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String questionId = entry.getKey();
            String answer = entry.getValue();
            String questionText = questions.get(questionId);

            // Don't cache API endpoint answers as they change every minute
            if (questionText != null && questionText.contains("endpoint API")) {
                logger.info("Skipping cache for API endpoint question {} - answer changes every minute", questionId);
                continue;
            }

            // Check if answer is already saved as correct in database
            String existingCorrectAnswer = getCachedAnswer(questionId);
            if (existingCorrectAnswer != null) {
                logger.info("Answer for question {} already saved as correct in database, skipping", questionId);
                continue;
            }

            // Update or insert answer as correct
            databaseService.executeUpdate(dbPath,
                    databaseQueryService.insertOrReplaceCorrectPhoneAnswer(),
                    questionId, answer);
            logger.info("Marked answer for question {} as correct: {}", questionId, answer);
        }
    }

    private void markPreviousAnswersAsCorrect(String problemQuestion, Map<String, String> currentAnswers) {
        try {
            int problemQuestionNum = Integer.parseInt(problemQuestion);

            for (Map.Entry<String, String> entry : currentAnswers.entrySet()) {
                String questionId = entry.getKey();
                String answer = entry.getValue();

                try {
                    int questionNum = Integer.parseInt(questionId);

                    // If this question is before the problematic one
                    if (questionNum < problemQuestionNum) {
                        // Check if it's already marked as correct
                        Object existingCorrect = databaseService.executeSingleValue(dbPath,
                                databaseQueryService.selectCorrectPhoneAnswerExists(),
                                questionId);

                        if (existingCorrect == null) {
                            // Mark as correct since it wasn't rejected
                            databaseService.executeUpdate(dbPath,
                                    databaseQueryService.insertOrReplaceCorrectPhoneAnswer(),
                                    questionId, answer);
                            logger.info("Marked answer for question {} as correct (before problem question {}): {}",
                                    questionId, problemQuestion, answer);
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse question number: {}", questionId);
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse problem question number: {}", problemQuestion);
        }
    }

    private void storeIncorrectAnswer(String questionId, String answer, String errorMessage, int attemptNumber) {
        // Store incorrect answer with is_correct = 0 flag
        databaseService.executeUpdate(dbPath,
                databaseQueryService.insertIncorrectPhoneAnswer(),
                questionId, answer);
        logger.info("Stored incorrect answer for question {} (attempt {}): {} -> Error: {}",
                questionId, attemptNumber, answer, errorMessage);
    }

    private List<Map<String, Object>> getIncorrectAnswersHistory(String questionId) {
        List<Map<String, Object>> history = databaseService.executeQuery(dbPath,
                databaseQueryService.selectIncorrectPhoneAnswers(),
                questionId);

        // Transform the results to match expected format
        List<Map<String, Object>> transformedHistory = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            Map<String, Object> entry = history.get(i);
            String answer = (String) entry.get("answer");

            Map<String, Object> transformedEntry = new HashMap<>();
            transformedEntry.put("answer", answer);
            transformedEntry.put("error_message", "Incorrect answer");
            transformedEntry.put("attempt_number", i + 1); // Sequential attempt numbers

            transformedHistory.add(transformedEntry);
        }

        return transformedHistory;
    }

    private String generateAnswerWithFeedback(String question, String questionId,
            List<Map<String, Object>> conversations, Map<String, String> facts) {

        // Handle API endpoint question (question 05) - special case
        if (question.contains("endpoint API")) {
            return handleApiEndpointQuestion(conversations);
        }

        // Get history of incorrect answers for this question
        List<Map<String, Object>> incorrectHistory = getIncorrectAnswersHistory(questionId);

        // Create feedback answer prompt using PromptService with incorrect history
        String prompt = promptService.w05d01_createQuestionAnswerWithFeedbackPrompt(
                question, questionId, conversations, facts, incorrectHistory);

        return openAiAdapter.getAnswer(prompt, "gpt-4o");
    }

    private String extractMessageFromJson(String jsonResponse) {
        // Extract message field from JSON response
        // Input: {"code": 0, "message": "39bef2f80ca8f42fe7d375dfcc1e2d05", "hint":
        // "This token changes every minute!"}
        // Output: "39bef2f80ca8f42fe7d375dfcc1e2d05"

        try {
            // Try to parse as JSON using ObjectMapper
            Map<String, Object> jsonMap = objectMapper.readValue(jsonResponse,
                    new TypeReference<Map<String, Object>>() {
                    });
            Object messageValue = jsonMap.get("message");
            if (messageValue != null) {
                return messageValue.toString();
            }
        } catch (Exception e) {
            logger.warn("Could not parse JSON response with ObjectMapper, trying manual extraction: {}",
                    e.getMessage());

            // Fallback: manual extraction
            return extractMessageManually(jsonResponse);
        }

        return null;
    }

    private String extractMessageManually(String jsonResponse) {
        // Manual extraction as fallback
        try {
            if (jsonResponse.contains("\"message\":")) {
                int messageStart = jsonResponse.indexOf("\"message\":");
                if (messageStart != -1) {
                    // Find the value after "message":
                    String afterMessage = jsonResponse.substring(messageStart + 10).trim();
                    if (afterMessage.startsWith("\"")) {
                        // Find the closing quote
                        int endQuote = afterMessage.indexOf("\"", 1);
                        if (endQuote != -1) {
                            return afterMessage.substring(1, endQuote);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error in manual message extraction: {}", e.getMessage());
        }

        return null;
    }

    private String extractCleanApiResponse(String fullResponse) {
        // Extract clean API response from error messages
        // Input: "Error calling API: Error during POST API call to
        // https://rafal.ag3nts.org/510bc: 403 : "{<EOL> "code": -200,<EOL> "message":
        // "Password incorrect! You are not RAV!"<EOL>}""
        // Output: "Password incorrect! You are not RAV!"

        try {
            // Look for JSON message in the error
            if (fullResponse.contains("\"message\":")) {
                int messageStart = fullResponse.indexOf("\"message\":");
                if (messageStart != -1) {
                    // Find the value after "message":
                    String afterMessage = fullResponse.substring(messageStart + 10).trim();
                    if (afterMessage.startsWith("\"")) {
                        // Find the closing quote
                        int endQuote = afterMessage.indexOf("\"", 1);
                        if (endQuote != -1) {
                            return afterMessage.substring(1, endQuote);
                        }
                    }
                }
            }

            // Fallback: return the original response if we can't extract the message
            return fullResponse;

        } catch (Exception e) {
            logger.warn("Error extracting clean API response: {}", e.getMessage());
            return fullResponse;
        }
    }

    private void ensureDatabaseInitialized() {
        try {
            databaseService.hasData(dbPath, "phone_conversations");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Database not initialized. Please call /weeks/5/days/1/init-db endpoint first to initialize the database.");
        }
    }

}