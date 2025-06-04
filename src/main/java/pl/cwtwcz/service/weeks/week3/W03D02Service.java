package pl.cwtwcz.service.weeks.week3;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FileService;
import pl.cwtwcz.service.QdrantService;
import pl.cwtwcz.service.FlagService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Service
public class W03D02Service {

    private static final Logger logger = LoggerFactory.getLogger(W03D02Service.class);

    @Value("${custom.w03d02.reports-dir}")
    private String reportsDir;

    @Value("${custom.w03d02.qdrant.collection}")
    private String collectionName;

    @Value("${custom.w03d02.embedding.model}")
    private String embeddingModel;

    @Value("${custom.w03d02.vector.size}")
    private int vectorSize;

    @Value("${custom.w03d02.query.text}")
    private String queryText;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${aidevs.api.key}")
    private String apiKey;

    private final OpenAiAdapter openAiAdapter;
    private final ApiExplorerService apiExplorerService;
    private final FileService fileService;
    private final QdrantService qdrantService;
    private final FlagService flagService;

    public String w03d02() {
        try {
            logger.info("Starting W03D02 - Vector search for weapon theft report");

            // Step 1. Create Qdrant collection
            logger.info("Creating Qdrant collection: {}", collectionName);
            qdrantService.createCollection(collectionName, vectorSize);

            // Step 2. Index all reports
            logger.info("Indexing reports from directory: {}", reportsDir);
            indexReports();

            // Step 3. Search for theft report
            logger.info("Searching for weapon theft report");
            String theftDate = findTheftReport();

            if (theftDate != null) {
                // Step 4. Send answer to centrala
                logger.info("Found theft report date: {}", theftDate);
                return sendAnswer(theftDate);
            } else {
                logger.error("Could not find theft report");
                return "ERROR: Theft report not found";
            }

        } catch (Exception e) {
            logger.error("Error in W03D02: {}", e.getMessage(), e);
            return "ERROR: " + e.getMessage();
        }
    }

    private void indexReports() {
        File reportsDirectory = new File(reportsDir);
        File[] reportFiles = reportsDirectory.listFiles((dir, name) -> name.endsWith(".txt"));

        if (reportFiles == null || reportFiles.length == 0) {
            throw new RuntimeException("No report files found in directory: " + reportsDir);
        }

        logger.info("Found {} report files to index", reportFiles.length);

        for (File reportFile : reportFiles) {
            // Step 5. Parse date from filename
            String filename = reportFile.getName();
            String date = parseDate(filename);

            // Step 6. Read report content
            String content = fileService.readStringFromFile(reportFile.getAbsolutePath());

            // Step 7. Generate embedding
            List<Float> embedding = openAiAdapter.createEmbedding(content, embeddingModel);

            // Step 8. Create metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("date", date);
            metadata.put("filename", filename);

            // Step 9. Index in Qdrant
            qdrantService.indexDocument(collectionName, filename, embedding, metadata);
            logger.debug("Indexed report: {} with date: {}", filename, date);
        }
    }

    private String parseDate(String filename) {
        // Remove .txt extension and replace underscores with dashes
        String dateStr = filename.replace(".txt", "").replace("_", "-");

        // Parse using DateTimeFormatter
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(dateStr, inputFormatter);

        // Return in the required format
        return date.format(inputFormatter);
    }

    private String findTheftReport() {
        // Step 10. Create query embedding
        List<Float> queryEmbedding = openAiAdapter.createEmbedding(queryText, embeddingModel);

        // Step 11. Search in Qdrant
        QdrantService.SearchResult result = qdrantService.searchSimilar(
                collectionName, queryEmbedding, 1);

        if (result != null) {
            String date = result.getPayloadValue("date");
            logger.info("Found matching report with date: {} (score: {})", date, result.getScore());
            return date;
        }

        return null;
    }

    private String sendAnswer(String date) {
        // Step 12. Prepare answer payload
        Map<String, String> answerPayload = new HashMap<>();
        answerPayload.put("task", "wektory");
        answerPayload.put("apikey", apiKey);
        answerPayload.put("answer", date);

        // Step 13. Send to centrala
        String response = apiExplorerService.postJsonForObject(reportUrl, answerPayload, String.class);

        logger.info("Sent answer to centrala. Response: {}", response);

        // Step 14. Check for flag in response using FlagService
        flagService.findFlagInText(response);

        return response;
    }
}