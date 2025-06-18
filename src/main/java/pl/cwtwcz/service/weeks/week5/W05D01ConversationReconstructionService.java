package pl.cwtwcz.service.weeks.week5;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.dto.week5.PhoneConversationDto;
import pl.cwtwcz.dto.week5.PhoneDataDto;
import pl.cwtwcz.service.DatabaseService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.PromptService;

import java.util.*;

@RequiredArgsConstructor
@Service
public class W05D01ConversationReconstructionService {

    private static final Logger logger = LoggerFactory.getLogger(W05D01ConversationReconstructionService.class);

    @Value("${custom.w05d01.phone.data.path}")
    private String phoneDataPath;

    @Value("${custom.w05d01.database.path}")
    private String dbPath;

    private final OpenAiAdapter openAiAdapter;
    private final FileService fileService;
    private final DatabaseService databaseService;
    private final ObjectMapper objectMapper;
    private final PromptService promptService;

    public String reconstructConversationsStep() {
        try {
            logger.info("Starting W05D01 - Conversations reconstruction step");

            // Step 1. Ensure database is initialized
            ensureDatabaseInitialized();

            // Step 2. Load phone data
            PhoneDataDto phoneData = loadPhoneData();

            // Step 3. Reconstruct conversations and store in database
            List<Map<String, Object>> conversations = reconstructConversations(phoneData);
            logger.info("Successfully reconstructed {} conversations and stored in database", conversations.size());

            // Step 4. Return summary
            StringBuilder result = new StringBuilder();
            result.append("Reconstructed ").append(conversations.size()).append(" conversations.");
            return result.toString();
        } catch (Exception e) {
            logger.error("Error processing W05D01 reconstruction step: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process W05D01 reconstruction step", e);
        }
    }

    public List<Map<String, Object>> getCachedConversations() {
        List<Map<String, Object>> rows = databaseService.executeQuery(dbPath,
                "SELECT content FROM phone_conversations WHERE id = 'reconstructed'");

        if (!rows.isEmpty()) {
            String content = (String) rows.get(0).get("content");
            return parseReconstructedConversations(content);
        }

        return new ArrayList<>();
    }

    public String initializeDatabase() {
        try {
            logger.info("Initializing database for W05D01 conversation reconstruction");
            databaseService.executeDDL(dbPath,
                    "CREATE TABLE IF NOT EXISTS phone_conversations (id TEXT PRIMARY KEY, content TEXT, analysis TEXT)");
            databaseService.executeDDL(dbPath,
                    "CREATE TABLE IF NOT EXISTS phone_answers (id INTEGER PRIMARY KEY AUTOINCREMENT, question_id TEXT, answer TEXT, is_correct BOOLEAN, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            logger.info("Database initialized successfully for W05D01");
            return "Database initialized successfully. Created tables: phone_conversations, phone_answers";

        } catch (Exception e) {
            logger.error("Error initializing database: {}", e.getMessage(), e);
            return "Error initializing database: " + e.getMessage();
        }
    }

    private void ensureDatabaseInitialized() {
        // Check if tables exist, if not throw exception
        try {
            databaseService.hasData(dbPath, "phone_conversations");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Database not initialized. Please call /weeks/5/days/1/init-db endpoint first to initialize the database.");
        }
    }

    private PhoneDataDto loadPhoneData() {
        try {
            String jsonContent = fileService.readStringFromFile(phoneDataPath);

            // Parse JSON manually due to specific structure
            Map<String, Object> rawData = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

            PhoneDataDto phoneData = new PhoneDataDto();
            phoneData.setConversations(new HashMap<>());

            // Extract conversations
            for (Map.Entry<String, Object> entry : rawData.entrySet()) {
                if (entry.getKey().startsWith("rozmowa")) {
                    Map<String, Object> convData = (Map<String, Object>) entry.getValue();
                    PhoneConversationDto conversation = new PhoneConversationDto();
                    conversation.setStart((String) convData.get("start"));
                    conversation.setEnd((String) convData.get("end"));
                    conversation.setLength((Integer) convData.get("length"));
                    phoneData.getConversations().put(entry.getKey(), conversation);
                }
            }

            // Extract remaining sentences
            if (rawData.containsKey("reszta")) {
                phoneData.setReszta((List<String>) rawData.get("reszta"));
            }

            logger.info("Loaded {} conversations and {} remaining sentences",
                    phoneData.getConversations().size(),
                    phoneData.getReszta() != null ? phoneData.getReszta().size() : 0);

            return phoneData;

        } catch (Exception e) {
            logger.error("Error loading phone data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load phone data", e);
        }
    }

    private List<Map<String, Object>> reconstructConversations(PhoneDataDto phoneData) {
        logger.info("Reconstructing conversations from fragments with feedback loop");

        // Check if conversations already reconstructed
        if (databaseService.hasData(dbPath, "phone_conversations")) {
            logger.info("Using cached reconstructed conversations");
            return getCachedConversations();
        }

        // Initial reconstruction
        List<Map<String, Object>> conversations = performInitialReconstruction(phoneData);

        // Save initial reconstruction
        String initialResult = conversationsToJson(conversations);
        databaseService.executeUpdate(dbPath,
                "INSERT OR REPLACE INTO phone_conversations (id, content, analysis) VALUES (?, ?, ?)",
                "iteration_0_initial", initialResult, "initial_reconstruction");
        logger.info("Saved initial reconstruction to database as iteration_0_initial");

        // Feedback loop - 10 iterations of improvements
        for (int iteration = 1; iteration <= 10; iteration++) {
            logger.info("Starting feedback loop iteration {}/10", iteration);

            try {
                var improvedConversations = improveReconstructionWithFeedback(conversations, phoneData, iteration);

                if (improvedConversations.isEmpty()) {
                    logger.warn("No improvement in iteration {}, keeping current version", iteration);
                    continue;
                } else {
                    String iterationResult = conversationsToJson(improvedConversations.orElse(conversations));
                    databaseService.executeUpdate(dbPath,
                            "INSERT OR REPLACE INTO phone_conversations (id, content, analysis) VALUES (?, ?, ?)",
                            "iteration_" + iteration, iterationResult, "feedback_iteration_" + iteration);
                    logger.info("Saved iteration {} reconstruction to database as iteration_{}", iteration, iteration);
                }
            } catch (Exception e) {
                logger.error("Error in feedback loop iteration {}: {}", iteration, e.getMessage(), e);
            }
        }

        // Store final result for caching (this is what getCachedConversations() will
        // use)
        String finalResult = conversationsToJson(conversations);
        databaseService.executeUpdate(dbPath,
                "INSERT OR REPLACE INTO phone_conversations (id, content, analysis) VALUES (?, ?, ?)",
                "reconstructed", finalResult, "feedback_loop_completed");

        logger.info("Successfully reconstructed {} conversations after feedback loop", conversations.size());
        return conversations;
    }

    private List<Map<String, Object>> performInitialReconstruction(PhoneDataDto phoneData) {
        logger.info("Performing initial reconstruction");

        // Prepare data for prompt service
        Map<String, Object> phoneDataMap = preparePhoneDataForPrompt(phoneData);

        // Create prompt using PromptService
        String prompt = promptService.w05d01_createInitialReconstructionPrompt(phoneDataMap);

        logger.info("Sending initial reconstruction prompt to AI");
        String reconstructionResult = openAiAdapter.getAnswer(prompt, "gpt-4o");
        logger.info("Received initial reconstruction result: {}",
                reconstructionResult.substring(0, Math.min(200, reconstructionResult.length())) + "...");

        // Parse reconstruction result
        List<Map<String, Object>> conversations = parseReconstructedConversations(reconstructionResult);
        logger.info("Initial reconstruction completed with {} conversations", conversations.size());
        return conversations;
    }

    private Map<String, Object> preparePhoneDataForPrompt(PhoneDataDto phoneData) {
        Map<String, Object> phoneDataMap = new HashMap<>();

        // Convert conversations to map format expected by PromptService
        Map<String, Map<String, Object>> conversationsMap = new HashMap<>();
        for (Map.Entry<String, PhoneConversationDto> entry : phoneData.getConversations().entrySet()) {
            PhoneConversationDto conv = entry.getValue();
            Map<String, Object> convMap = new HashMap<>();
            convMap.put("start", conv.getStart());
            convMap.put("end", conv.getEnd());
            convMap.put("length", conv.getLength());
            conversationsMap.put(entry.getKey(), convMap);
        }

        phoneDataMap.put("conversations", conversationsMap);
        phoneDataMap.put("remainingSentences", phoneData.getReszta());

        return phoneDataMap;
    }

    private Optional<List<Map<String, Object>>> improveReconstructionWithFeedback(
            List<Map<String, Object>> currentConversations,
            PhoneDataDto phoneData, int iteration) {

        logger.info("Improving reconstruction with feedback - iteration {}", iteration);

        // First, validate current reconstruction and identify issues
        StringBuilder validationReport = validateReconstructionLengths(currentConversations, phoneData);

        // Prepare data for prompt service
        Map<String, Object> phoneDataMap = preparePhoneDataForPrompt(phoneData);

        // Create feedback improvement prompt using PromptService
        String prompt = promptService.w05d01_createFeedbackImprovementPrompt(
                currentConversations, phoneDataMap, validationReport.toString(), iteration);

        logger.info("Sending feedback improvement prompt to AI - iteration {}", iteration);
        String improvementResult = openAiAdapter.getAnswer(prompt, "gpt-4o");
        logger.info("Received improvement result for iteration {}: {}", iteration,
                improvementResult.substring(0, Math.min(200, improvementResult.length())) + "...");

        // Parse improved reconstruction
        List<Map<String, Object>> improvedConversations = parseReconstructedConversations(improvementResult);

        if (improvedConversations.isEmpty()) {
            logger.warn("Failed to parse improved reconstruction in iteration {}, keeping current version", iteration);
            return Optional.empty();
        }

        // Validate improved reconstruction
        StringBuilder newValidationReport = validateReconstructionLengths(improvedConversations, phoneData);
        logger.info("Validation report for iteration {}: {}", iteration, newValidationReport.toString());

        // Check if improvement is actually better
        if (isReconstructionValid(improvedConversations, phoneData)) {
            logger.info("Iteration {} completed successfully - improved {} conversations", iteration,
                    improvedConversations.size());
            return Optional.of(improvedConversations);
        } else {
            logger.warn("Iteration {} failed validation, keeping current version", iteration);
            return Optional.empty();
        }
    }

    private StringBuilder validateReconstructionLengths(List<Map<String, Object>> conversations,
            PhoneDataDto phoneData) {
        StringBuilder report = new StringBuilder();

        // Check conversation lengths
        for (Map<String, Object> conv : conversations) {
            String convName = (String) conv.get("name");
            List<String> sentences = (List<String>) conv.get("sentences");
            int actualLength = sentences != null ? sentences.size() : 0;

            PhoneConversationDto expectedConv = phoneData.getConversations().get(convName);
            if (expectedConv != null) {
                int expectedLength = expectedConv.getLength();
                if (actualLength != expectedLength) {
                    report.append(String.format("❌ %s: ma %d zdań, powinno być %d (różnica: %+d)\n",
                            convName, actualLength, expectedLength, actualLength - expectedLength));
                } else {
                    report.append(String.format("✅ %s: poprawna długość %d zdań\n", convName, actualLength));
                }
            }
        }

        // Check for duplicate sentences
        Set<String> usedSentences = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        int totalSentences = 0;

        for (Map<String, Object> conv : conversations) {
            List<String> sentences = (List<String>) conv.get("sentences");
            if (sentences != null) {
                for (String sentence : sentences) {
                    totalSentences++;
                    if (usedSentences.contains(sentence)) {
                        duplicates.add(sentence);
                    } else {
                        usedSentences.add(sentence);
                    }
                }
            }
        }

        report.append("\nSPRAWDZENIE DUPLIKATÓW:\n");
        if (duplicates.isEmpty()) {
            report.append(String.format("✅ Brak duplikatów - wszystkie %d zdań są unikalne\n", usedSentences.size()));
        } else {
            report.append(String.format("❌ Znaleziono %d duplikatów:\n", duplicates.size()));
            for (String duplicate : duplicates) {
                report.append(String.format("   - \"%s\"\n", duplicate));
            }
        }

        // Check total count
        int expectedTotal = phoneData.getConversations().values().stream()
                .mapToInt(PhoneConversationDto::getLength)
                .sum();

        report.append("\nSPRAWDZENIE LICZBY ZDAŃ:\n");
        if (totalSentences == expectedTotal) {
            report.append(String.format("✅ Poprawna liczba zdań: %d\n", totalSentences));
        } else {
            report.append(String.format("❌ Niepoprawna liczba zdań: %d (oczekiwano %d, różnica: %+d)\n",
                    totalSentences, expectedTotal, totalSentences - expectedTotal));
        }

        return report;
    }

    private boolean isReconstructionValid(List<Map<String, Object>> conversations, PhoneDataDto phoneData) {
        // Check for duplicate sentences across all conversations
        Set<String> usedSentences = new HashSet<>();
        List<String> allSentences = new ArrayList<>();

        for (Map<String, Object> conv : conversations) {
            String convName = (String) conv.get("name");
            List<String> sentences = (List<String>) conv.get("sentences");
            int actualLength = sentences != null ? sentences.size() : 0;

            if (sentences != null) {
                for (String sentence : sentences) {
                    allSentences.add(sentence);
                    if (usedSentences.contains(sentence)) {
                        logger.warn("Validation failed: Duplicate sentence found: '{}'", sentence);
                        return false;
                    }
                    usedSentences.add(sentence);
                }
            }

            PhoneConversationDto expectedConv = phoneData.getConversations().get(convName);
            if (expectedConv != null) {
                int expectedLength = expectedConv.getLength();
                if (actualLength != expectedLength) {
                    logger.warn("Validation failed: {} has {} sentences, expected {}", convName, actualLength,
                            expectedLength);
                    return false;
                }

                // Check first and last sentences
                if (sentences != null && !sentences.isEmpty()) {
                    String firstSentence = sentences.get(0);
                    String lastSentence = sentences.get(sentences.size() - 1);

                    if (!firstSentence.equals(expectedConv.getStart())) {
                        logger.warn("Validation failed: {} first sentence doesn't match. Expected: '{}', Got: '{}'",
                                convName, expectedConv.getStart(), firstSentence);
                        return false;
                    }

                    if (!lastSentence.equals(expectedConv.getEnd())) {
                        logger.warn("Validation failed: {} last sentence doesn't match. Expected: '{}', Got: '{}'",
                                convName, expectedConv.getEnd(), lastSentence);
                        return false;
                    }
                }
            }
        }

        // Check if all input sentences are used (count validation)
        int totalExpectedSentences = phoneData.getConversations().values().stream()
                .mapToInt(PhoneConversationDto::getLength)
                .sum();

        if (allSentences.size() != totalExpectedSentences) {
            logger.warn("Validation failed: Total sentences count mismatch. Expected: {}, Got: {}",
                    totalExpectedSentences, allSentences.size());
            return false;
        }

        logger.info("Validation passed: {} unique sentences, no duplicates found", usedSentences.size());
        return true;
    }

    private String conversationsToJson(List<Map<String, Object>> conversations) {
        try {
            Map<String, Object> jsonMap = new HashMap<>();
            for (Map<String, Object> conv : conversations) {
                jsonMap.put((String) conv.get("name"), conv.get("sentences"));
            }
            return objectMapper.writeValueAsString(jsonMap);
        } catch (Exception e) {
            logger.error("Error converting conversations to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    private List<Map<String, Object>> parseReconstructedConversations(String reconstructionResult) {
        List<Map<String, Object>> conversations = new ArrayList<>();

        try {
            // First, try to extract JSON from the response (it might be wrapped in text)
            String jsonContent = extractJsonFromResponse(reconstructionResult);

            if (jsonContent != null) {
                // Parse as clean JSON
                Map<String, Object> jsonResult = objectMapper.readValue(jsonContent,
                        new TypeReference<Map<String, Object>>() {
                        });

                for (Map.Entry<String, Object> entry : jsonResult.entrySet()) {
                    if (entry.getKey().startsWith("rozmowa")) {
                        Map<String, Object> conversation = new HashMap<>();
                        conversation.put("name", entry.getKey());

                        // Handle the case where value is a List of sentences
                        if (entry.getValue() instanceof List) {
                            conversation.put("sentences", entry.getValue());
                        } else {
                            logger.warn("Unexpected value type for conversation {}: {}", entry.getKey(),
                                    entry.getValue().getClass());
                            continue;
                        }
                        conversations.add(conversation);
                    }
                }
            } else {
                // Fallback: Parse as text format
                String[] lines = reconstructionResult.split("\n");
                Map<String, Object> currentConversation = null;
                List<String> currentSentences = new ArrayList<>();

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("rozmowa")) {
                        // Save previous conversation
                        if (currentConversation != null) {
                            currentConversation.put("sentences", new ArrayList<>(currentSentences));
                            conversations.add(currentConversation);
                        }

                        // Start new conversation
                        currentConversation = new HashMap<>();
                        currentConversation.put("name", line.split(":")[0]);
                        currentSentences.clear();
                    } else if (!line.isEmpty() && line.startsWith("\"") && line.endsWith("\"")) {
                        currentSentences.add(line.substring(1, line.length() - 1));
                    }
                }

                // Add last conversation
                if (currentConversation != null) {
                    currentConversation.put("sentences", new ArrayList<>(currentSentences));
                    conversations.add(currentConversation);
                }
            }

            logger.info("Successfully parsed {} conversations from reconstruction result", conversations.size());

        } catch (Exception e) {
            logger.error("Error parsing reconstruction result: {}", e.getMessage());
            logger.debug("Failed reconstruction result content: {}",
                    reconstructionResult.substring(0, Math.min(500, reconstructionResult.length())));
            // Return empty list if parsing fails
        }

        return conversations;
    }

    private String extractJsonFromResponse(String response) {
        try {
            // Look for JSON block starting with { and ending with }
            int startIndex = response.indexOf('{');
            if (startIndex == -1) {
                return null;
            }

            int braceCount = 0;
            int endIndex = -1;

            for (int i = startIndex; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIndex = i;
                        break;
                    }
                }
            }

            if (endIndex != -1) {
                String jsonContent = response.substring(startIndex, endIndex + 1);
                logger.debug("Extracted JSON content: {}",
                        jsonContent.substring(0, Math.min(200, jsonContent.length())) + "...");
                return jsonContent;
            }

        } catch (Exception e) {
            logger.warn("Error extracting JSON from response: {}", e.getMessage());
        }

        return null;
    }
}