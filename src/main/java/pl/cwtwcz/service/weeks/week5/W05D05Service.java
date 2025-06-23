package pl.cwtwcz.service.weeks.week5;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.service.DatabaseService;
import pl.cwtwcz.service.DatabaseQueryService;
import pl.cwtwcz.dto.week5.W05D05StoryRequestDto;
import pl.cwtwcz.dto.week5.W05D05CentralaResponseDto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class W05D05Service {

    private static final Logger logger = LoggerFactory.getLogger(W05D05Service.class);
    private static final int MAX_ITERATIONS = 5;
    private static final int LLM_DELAY_SECONDS = 10;

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${centrala.base.url}")
    private String centralaBaseUrl;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${custom.w05d05.database.path}")
    private String dbPath;

    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;
    private final FileService fileService;
    private final ApiExplorerService apiExplorerService;
    private final FlagService flagService;
    private final DatabaseService databaseService;
    private final DatabaseQueryService databaseQueryService;
    private final ObjectMapper objectMapper;

    public String w05d05() {
        try {
            logger.info("Starting W05D05 task - Story compilation and chronology");

            // Step 1. Initialize database
            logger.info("Step 1. Initializing database");
            initializeDatabase();

            // Step 2. Download questions from centrala
            logger.info("Step 2. Downloading questions from centrala");
            List<String> questions = downloadStoryQuestions();
            logger.info("Retrieved {} questions from centrala", questions.size());

            // Step 3. Build knowledge base from all materials
            logger.info("Step 3. Building knowledge base from historical materials");
            String knowledgeBase = buildKnowledgeBase();

            // Step 4. Answer questions with feedback loop using database
            logger.info("Step 4. Starting feedback loop for answering questions");
            String finalResponse = answerQuestionsWithFeedbackLoop(questions, knowledgeBase);

            logger.info("W05D05 task completed successfully: {}", finalResponse);

            // Step 5. Extract flag from response
            return flagService.findFlagInText(finalResponse)
                    .orElse("Task completed. Response: " + finalResponse);

        } catch (Exception e) {
            logger.error("Error in W05D05 task: ", e);
            throw new RuntimeException("Failed to complete W05D05 task", e);
        }
    }

    private void initializeDatabase() {
        try {
            // Step 1. Ensure database directory exists
            String dbDirectory = getParentDirectory(dbPath);
            if (dbDirectory != null && !dbDirectory.isEmpty()) {
                fileService.createDirectories(dbDirectory);
                logger.info("Database directory ensured: {}", dbDirectory);
            } else {
                logger.info("Database will be created in current directory: {}", dbPath);
            }

            // Step 2. Create story_answers table
            databaseService.executeUpdate(dbPath, databaseQueryService.createStoryAnswersTable());
            logger.info("Database initialized successfully at: {}", dbPath);
        } catch (Exception e) {
            logger.error("Error initializing database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private String getParentDirectory(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }
        
        // Handle both Windows (\) and Unix (/) path separators
        int lastSlashIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        
        if (lastSlashIndex == -1) {
            // No path separator found, file is in current directory
            return null;
        }
        
        return filePath.substring(0, lastSlashIndex);
    }

    private String answerQuestionsWithFeedbackLoop(List<String> questions, String knowledgeBase) {
        String lastResponse = null;

        for (int iteration = 1; iteration <= MAX_ITERATIONS; iteration++) {
            logger.info("=== FEEDBACK LOOP ITERATION {} ===", iteration);

            // Step 4.1. Generate answers for questions we don't have correct answers for
            List<String> currentAnswers = generateAnswersForIteration(questions, knowledgeBase);

            // Step 4.2. Send answers to centrala
            logger.info("Sending {} answers to centrala (iteration {})", currentAnswers.size(), iteration);
            lastResponse = sendAnswersToCentrala(currentAnswers);

            // Step 4.3. Parse response and update database
            if (parseResponseAndUpdateDatabase(lastResponse, currentAnswers)) {
                logger.info("All answers are correct! Task completed successfully.");
                break;
            }

            // Step 4.4. Log current progress
            logProgress(questions.size());

            if (iteration < MAX_ITERATIONS) {
                logger.info("Proceeding to next iteration...");
            }
        }

        return lastResponse;
    }

    private List<String> generateAnswersForIteration(List<String> questions, String knowledgeBase) {
        List<String> answers = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            String correctAnswer = getCachedCorrectAnswer(i);

            if (correctAnswer != null) {
                // Use correct answer from database
                answers.add(correctAnswer);
                logger.info("Using cached correct answer for question {}: {}", i, correctAnswer);
            } else {
                // Generate new answer for this question
                String question = questions.get(i);
                Set<String> previousIncorrectAnswers = getCachedIncorrectAnswers(i);

                logger.info("Generating answer for question {} (attempt with {} previous incorrect answers)",
                        i, previousIncorrectAnswers.size());

                String answer = generateSingleAnswer(question, knowledgeBase, previousIncorrectAnswers);
                answers.add(answer);

                logger.info("Generated answer for question {}: {}", i, answer);

                // Add delay to avoid rate limiting
                sleepBetweenLLMCalls();
            }
        }

        return answers;
    }

    private String getCachedCorrectAnswer(int questionIndex) {
        try {
            Object result = databaseService.executeSingleValue(dbPath,
                    databaseQueryService.selectCorrectStoryAnswer(), questionIndex);
            return result != null ? (String) result : null;
        } catch (Exception e) {
            logger.warn("Error getting cached correct answer for question {}: {}", questionIndex, e.getMessage());
            return null;
        }
    }

    private Set<String> getCachedIncorrectAnswers(int questionIndex) {
        try {
            List<Map<String, Object>> results = databaseService.executeQuery(dbPath,
                    databaseQueryService.selectIncorrectStoryAnswers(), questionIndex);

            return results.stream()
                    .map(row -> (String) row.get("answer"))
                    .filter(answer -> answer != null && !answer.trim().isEmpty())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.warn("Error getting cached incorrect answers for question {}: {}", questionIndex, e.getMessage());
            return new HashSet<>();
        }
    }

    private String generateSingleAnswer(String question, String knowledgeBase, Set<String> previousIncorrectAnswers) {
        // Create enhanced prompt with previous incorrect answers
        String prompt;
        if (previousIncorrectAnswers.isEmpty()) {
            logger.info("Using basic prompt (no previous incorrect answers)");
            prompt = promptService.w05d05_createStoryAnsweringPrompt(question, knowledgeBase);
        } else {
            logger.info("Using feedback prompt with {} previous incorrect answers: {}", 
                    previousIncorrectAnswers.size(), previousIncorrectAnswers);
            prompt = promptService.w05d05_createStoryAnsweringPromptWithFeedback(question, knowledgeBase,
                    previousIncorrectAnswers);
        }

        // Get answer from AI
        String answer = openAiAdapter.getAnswer(prompt, "gpt-4o-mini");

        // Clean up the answer
        answer = answer.trim();
        if (answer.startsWith("\"") && answer.endsWith("\"")) {
            answer = answer.substring(1, answer.length() - 1);
        }

        // Check if generated answer is the same as a previous incorrect one
        if (previousIncorrectAnswers.contains(answer)) {
            logger.warn("⚠️ AI generated the same incorrect answer again: '{}'. This suggests the feedback prompt needs improvement!", answer);
        }

        return answer;
    }

    private boolean parseResponseAndUpdateDatabase(String response, List<String> currentAnswers) {
        try {
            W05D05CentralaResponseDto responseDto = objectMapper.readValue(response, W05D05CentralaResponseDto.class);

            // Parse correct answers (ok field)
            if (responseDto.getOk() != null) {
                for (String correctEntry : responseDto.getOk()) {
                    parseAndStoreCorrectAnswer(correctEntry);
                }
            }

            // Parse incorrect answers (failed field)
            if (responseDto.getFailed() != null) {
                for (String failedEntry : responseDto.getFailed()) {
                    parseAndStoreIncorrectAnswer(failedEntry, currentAnswers);
                }
            }

            // Check if we have all correct answers
            int totalQuestions = currentAnswers.size();
            int correctCount = countCorrectAnswers();

            logger.info("Progress: {}/{} questions answered correctly", correctCount, totalQuestions);

            // Step 4.3.1. If all answers are correct, save them to database for future use
            boolean allAnswersCorrect = correctCount == totalQuestions;
            if (allAnswersCorrect) {
                logger.info("All answers are correct! Saving all answers to database for future runs.");
                saveAllAnswersAsCorrect(currentAnswers);
            }

            return allAnswersCorrect;

        } catch (Exception e) {
            logger.error("Error parsing response: {}", e.getMessage(), e);
            return false;
        }
    }

    private void parseAndStoreCorrectAnswer(String correctEntry) {
        // Format: "index[X] = answer"
        if (correctEntry.startsWith("index[") && correctEntry.contains("] = ")) {
            try {
                int indexStart = correctEntry.indexOf("[") + 1;
                int indexEnd = correctEntry.indexOf("]");
                int answerStart = correctEntry.indexOf(" = ") + 3;

                int index = Integer.parseInt(correctEntry.substring(indexStart, indexEnd));
                String answer = correctEntry.substring(answerStart);

                // Check if correct answer already exists in database
                String existingCorrectAnswer = getCachedCorrectAnswer(index);
                if (existingCorrectAnswer != null) {
                    logger.info("Correct answer for index {} already exists in database: {}", index,
                            existingCorrectAnswer);
                    return; // Don't store again
                }

                // Store new correct answer in database
                databaseService.executeUpdate(dbPath,
                        databaseQueryService.insertOrReplaceCorrectStoryAnswer(),
                        index, answer);

                // Clean up incorrect answers for this question since we now have the correct
                // one
                databaseService.executeUpdate(dbPath,
                        databaseQueryService.deleteIncorrectStoryAnswers(),
                        index);

                logger.info("Stored new correct answer for index {}: {}", index, answer);
            } catch (Exception e) {
                logger.warn("Could not parse correct entry: {}", correctEntry);
            }
        }
    }

    private void parseAndStoreIncorrectAnswer(String failedEntry, List<String> currentAnswers) {
        // Format: "index[X] = Error: ..." or "index[X] = wrong_answer"
        if (failedEntry.startsWith("index[") && failedEntry.contains("] = ")) {
            try {
                int indexStart = failedEntry.indexOf("[") + 1;
                int indexEnd = failedEntry.indexOf("]");

                int index = Integer.parseInt(failedEntry.substring(indexStart, indexEnd));

                // Get the actual answer we sent (not the error message)
                if (index < currentAnswers.size()) {
                    String incorrectAnswer = currentAnswers.get(index);

                    // Check if this incorrect answer was already stored to avoid duplicates
                    Set<String> existingIncorrectAnswers = getCachedIncorrectAnswers(index);
                    if (existingIncorrectAnswers.contains(incorrectAnswer)) {
                        logger.info("Incorrect answer for index {} already exists in database: {}", index, incorrectAnswer);
                        return; // Don't store duplicate
                    }

                    // Store in database
                    databaseService.executeUpdate(dbPath,
                            databaseQueryService.insertIncorrectStoryAnswer(),
                            index, incorrectAnswer);

                    logger.info("Stored new incorrect answer for index {}: {}", index, incorrectAnswer);
                }
            } catch (Exception e) {
                logger.warn("Could not parse failed entry: {}", failedEntry);
            }
        }
    }

    private void saveAllAnswersAsCorrect(List<String> currentAnswers) {
        logger.info("Saving all {} answers as correct for future runs", currentAnswers.size());

        for (int i = 0; i < currentAnswers.size(); i++) {
            try {
                String answer = currentAnswers.get(i);

                // Check if correct answer already exists in database
                String existingCorrectAnswer = getCachedCorrectAnswer(i);
                if (existingCorrectAnswer != null) {
                    logger.info("Correct answer for index {} already exists in database: {}", i, existingCorrectAnswer);
                    continue; // Skip if already exists
                }

                // Store new correct answer in database
                databaseService.executeUpdate(dbPath,
                        databaseQueryService.insertOrReplaceCorrectStoryAnswer(),
                        i, answer);

                // Clean up incorrect answers for this question since we now have the correct
                // one
                databaseService.executeUpdate(dbPath,
                        databaseQueryService.deleteIncorrectStoryAnswers(),
                        i);

                logger.info("Saved correct answer for index {}: {}", i, answer);

            } catch (Exception e) {
                logger.warn("Could not save correct answer for index {}: {}", i, e.getMessage());
            }
        }

        logger.info("Completed saving all answers as correct");
    }

    private int countCorrectAnswers() {
        try {
            Object result = databaseService.executeSingleValue(dbPath,
                    databaseQueryService.countCorrectStoryAnswers());
            return result != null ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            logger.warn("Error counting correct answers: {}", e.getMessage());
            return 0;
        }
    }

    private void logProgress(int totalQuestions) {
        int correctCount = countCorrectAnswers();

        // Count total incorrect attempts
        int incorrectCount = 0;
        try {
            Object result = databaseService.executeSingleValue(dbPath,
                    databaseQueryService.countIncorrectStoryAnswers());
            incorrectCount = result != null ? ((Number) result).intValue() : 0;
        } catch (Exception e) {
            logger.warn("Error counting incorrect answers: {}", e.getMessage());
        }

        logger.info("Current progress: {}/{} correct answers, {} total incorrect attempts",
                correctCount, totalQuestions, incorrectCount);

        // Log which questions still need answers
        for (int i = 0; i < totalQuestions; i++) {
            if (getCachedCorrectAnswer(i) == null) {
                Set<String> incorrects = getCachedIncorrectAnswers(i);
                logger.info("Question {} still needs answer (tried {} incorrect answers)", i, incorrects.size());
            }
        }
    }

    private void sleepBetweenLLMCalls() {
        try {
            logger.info("Sleeping {} seconds to avoid rate limiting...", LLM_DELAY_SECONDS);
            Thread.sleep(LLM_DELAY_SECONDS * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Sleep interrupted");
        }
    }

    private List<String> downloadStoryQuestions() {
        String questionsUrl = centralaBaseUrl + "data/" + apiKey + "/story.json";
        logger.info("Downloading questions from: {}", questionsUrl);

        try {
            String response = apiExplorerService.postJsonForObject(questionsUrl, null, String.class);

            // Parse the response - it should be a JSON array of strings
            // Since we don't have exact format, let's handle it robustly
            if (response.startsWith("[") && response.endsWith("]")) {
                // Remove brackets and split by comma, handling quotes
                response = response.substring(1, response.length() - 1);
                List<String> questions = new ArrayList<>();

                // Simple parsing - split by ", " and remove quotes
                String[] parts = response.split("\",\\s*\"");
                for (String part : parts) {
                    String question = part.replaceAll("^\"|\"$", "").trim();
                    if (!question.isEmpty()) {
                        questions.add(question);
                    }
                }

                return questions;
            } else {
                // Try to parse as object with questions field
                return List.of(response); // Fallback
            }
        } catch (Exception e) {
            logger.error("Error downloading questions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download questions from centrala", e);
        }
    }

    private String buildKnowledgeBase() {
        StringBuilder knowledgeBase = new StringBuilder();

        knowledgeBase.append("=== BAZA WIEDZY - KOMPLETNA HISTORIA ZADAŃ AI DEVS ===\n\n");

        // Step 1. NAJWAŻNIEJSZE - Read Zygfryd's notebook pages (najnowsze jako
        // pierwsze)
        try {
            knowledgeBase.append("🏆 === NAJNOWSZE NOTATKI ZYGFRYDA (NAJWYŻSZY PRIORYTET) ===\n");
            String notebookDir = "C:/cache/w05d05/zygfryd_notatnik";
            Map<String, String> notebookFiles = fileService.listFilesInDirectory(notebookDir, "", null);

            // Sort files by number in filename (highest first)
            notebookFiles.entrySet().stream()
                    .sorted((a, b) -> {
                        // Extract number from filename
                        try {
                            String numA = a.getKey().replaceAll("\\D+", "");
                            String numB = b.getKey().replaceAll("\\D+", "");
                            if (numA.isEmpty() || numB.isEmpty())
                                return 0;
                            return Integer.compare(Integer.parseInt(numB), Integer.parseInt(numA)); // Descending
                        } catch (NumberFormatException e) {
                            return 0;
                        }
                    })
                    .forEach(entry -> {
                        String filename = entry.getKey();
                        String filePath = entry.getValue();
                        try {
                            String content = fileService.readStringFromFile(filePath);
                            knowledgeBase.append("🥇 --- NOTATKA ZYGFRYDA: ").append(filename).append(" ---\n");
                            knowledgeBase.append(content).append("\n\n");
                        } catch (Exception e) {
                            logger.warn("Could not read notebook file: {}", filename);
                        }
                    });
        } catch (Exception e) {
            logger.warn("Could not access notebook directory: {}", e.getMessage());
        }

        // Step 2. Read phone conversations
        try {
            knowledgeBase.append("📞 === ROZMOWY TELEFONICZNE - KLUCZOWE ASPEKTY MISJI ===\n");
            String phoneContent = fileService.readStringFromFile("C:/cache/w05d05/phone_sorted.json");
            knowledgeBase.append(phoneContent).append("\n\n");
        } catch (Exception e) {
            logger.warn("Could not read phone conversations: {}", e.getMessage());
        }

        // Step 3. Read interrogation transcripts about Andrzej Maj
        try {
            knowledgeBase.append("🔍 === PRZESŁUCHANIA DOTYCZĄCE ANDRZEJA MAJA ===\n");
            String interrogationsDir = "C:/cache/w05d05/przesluchania";
            Map<String, String> interrogationFiles = fileService.listFilesInDirectory(interrogationsDir, "", null);

            for (Map.Entry<String, String> entry : interrogationFiles.entrySet()) {
                String filename = entry.getKey();
                String filePath = entry.getValue();
                try {
                    String content = fileService.readStringFromFile(filePath);
                    knowledgeBase.append("--- PRZESŁUCHANIE: ").append(filename).append(" ---\n");
                    knowledgeBase.append(content).append("\n\n");
                } catch (Exception e) {
                    logger.warn("Could not read interrogation file: {}", filename);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not access interrogations directory: {}", e.getMessage());
        }

        // Step 4. Read factory reports
        try {
            knowledgeBase.append("🏭 === RAPORTY Z FABRYKI ROBOTÓW ===\n");
            String factoryReportsDir = "C:/cache/w05d05/raporty_z_fabryki";
            Map<String, String> factoryFiles = fileService.listFilesInDirectory(factoryReportsDir, "", null);

            for (Map.Entry<String, String> entry : factoryFiles.entrySet()) {
                String filename = entry.getKey();
                String filePath = entry.getValue();
                try {
                    String content = fileService.readStringFromFile(filePath);
                    knowledgeBase.append("--- RAPORT: ").append(filename).append(" ---\n");
                    knowledgeBase.append(content).append("\n\n");
                } catch (Exception e) {
                    logger.warn("Could not read factory report: {}", filename);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not access factory reports directory: {}", e.getMessage());
        }

        // Step 5. Read facts about people and sectors
        try {
            knowledgeBase.append("📄 === FAKTY O OSOBACH I SEKTORACH ===\n");
            String factsDir = "C:/cache/w05d05/fakty";
            Map<String, String> factsFiles = fileService.listFilesInDirectory(factsDir, "", null);

            for (Map.Entry<String, String> entry : factsFiles.entrySet()) {
                String filename = entry.getKey();
                String filePath = entry.getValue();
                try {
                    String content = fileService.readStringFromFile(filePath);
                    knowledgeBase.append("--- FAKT: ").append(filename).append(" ---\n");
                    knowledgeBase.append(content).append("\n\n");
                } catch (Exception e) {
                    logger.warn("Could not read fact file: {}", filename);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not access facts directory: {}", e.getMessage());
        }

        // Step 6. Read Andrzej Maj's scientific article about time travel
        try {
            knowledgeBase.append("📚 === ARTYKUŁ NAUKOWY ANDRZEJA MAJA O PODRÓŻACH W CZASIE ===\n");
            String arxivDir = "C:/cache/w05d05/axiv";
            Map<String, String> arxivFiles = fileService.listFilesInDirectory(arxivDir, "", null);

            for (Map.Entry<String, String> entry : arxivFiles.entrySet()) {
                String filename = entry.getKey();
                String filePath = entry.getValue();
                try {
                    String content = fileService.readStringFromFile(filePath);
                    knowledgeBase.append("--- ARTYKUŁ: ").append(filename).append(" ---\n");
                    knowledgeBase.append(content).append("\n\n");
                } catch (Exception e) {
                    logger.warn("Could not read arxiv file: {}", filename);
                }
            }
        } catch (Exception e) {
            logger.warn("Could not access arxiv directory: {}", e.getMessage());
        }

        // Step 7. Read Rafał's blog
        try {
            knowledgeBase.append("📰 === BLOG RAFAŁA ===\n");
            String rafalBlog = fileService.readStringFromFile("C:/cache/w05d05/rafal_blog.txt");
            knowledgeBase.append(rafalBlog).append("\n\n");
        } catch (Exception e) {
            logger.warn("Could not read Rafał's blog: {}", e.getMessage());
        }

        // Step 8. Read Softo AI blog
        try {
            knowledgeBase.append("🤖 === BLOG SOFTO AI ===\n");
            String softoBlog = fileService.readStringFromFile("C:/cache/w05d05/softo.txt");
            knowledgeBase.append(softoBlog).append("\n\n");
        } catch (Exception e) {
            logger.warn("Could not read Softo AI blog: {}", e.getMessage());
        }

        // Summary chronology for context
        knowledgeBase.append("📋 === CHRONOLOGIA ZADAŃ (KONTEKST) ===\n");
        knowledgeBase.append("TYDZIEŃ 1: Nawiązanie kontaktu z centralą, weryfikacja wiadomości\n");
        knowledgeBase.append("TYDZIEŃ 2: Przetwarzanie plików (adam.txt, mapy, raporty z sektorów), artykuł ArXiv\n");
        knowledgeBase.append("TYDZIEŃ 3: Bazy danych, wyszukiwanie wektorowe, poszukiwanie Barbary Zawadzkiej\n");
        knowledgeBase.append("TYDZIEŃ 4: Analiza zdjęć Barbary, weryfikacja badań, SOFTO, nawigacja dronem\n");
        knowledgeBase.append("TYDZIEŃ 5: Rekonstrukcja rozmów, GPS, podpisy cyfrowe, kompilacja historii\n\n");

        knowledgeBase.append("👥 === KLUCZOWE POSTACIE ===\n");
        knowledgeBase.append("⚠️ UWAGA: Dla informacji o postaciach sprawdź NAJNOWSZE notatki Zygfryda!\n");
        knowledgeBase.append("- Andrzej Maj: Naukowiec, autor artykułu o podróżach w czasie\n");
        knowledgeBase.append("- Barbara Zawadzka: Osoba poszukiwana, pojawiała się w raportach\n");
        knowledgeBase.append("- Zygfryd: Autor notatnika z ważnymi informacjami\n");
        knowledgeBase.append("- Rafał: Sprawdź stan w NAJNOWSZYCH notatkach Zygfryda!\n");
        knowledgeBase.append("- Aleksander Ragowski: Wykryty w sektorze C4\n\n");

        return knowledgeBase.toString();
    }

    private String sendAnswersToCentrala(List<String> answers) {
        // Create request with answers as JSON array
        W05D05StoryRequestDto request = new W05D05StoryRequestDto("story", apiKey, answers);

        logger.info("Sending {} answers to centrala", answers.size());

        // Send to centrala
        return apiExplorerService.postJsonForObject(reportUrl, request, String.class);
    }
}