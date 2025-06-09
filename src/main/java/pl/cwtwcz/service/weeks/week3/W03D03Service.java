package pl.cwtwcz.service.weeks.week3;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.dto.week3.DatabaseQueryRequestDto;
import pl.cwtwcz.dto.week3.DatabaseReportRequestDto;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Service
public class W03D03Service {

    private static final Logger logger = LoggerFactory.getLogger(W03D03Service.class);

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${custom.w03d03.database.api.url}")
    private String databaseApiUrl;

    private final OpenAiAdapter openAiAdapter;
    private final ApiExplorerService apiExplorerService;
    private final PromptService promptService;
    private final FlagService flagService;
    private final ObjectMapper objectMapper;

    public String w03d03() {
        try {
            logger.info("Starting W03D03 task - database query for active datacenters with inactive managers");

            // Step 1. Discover database structure
            List<String> tableNames = discoverTables();
            logger.info("Found tables: {}", tableNames);

            // Step 2. Get table schemas
            String databaseSchemas = getTableSchemas(tableNames);
            logger.info("Database schemas retrieved");

            // Step 3. Generate SQL query using LLM
            String sqlQuery = generateSqlQuery(databaseSchemas);
            logger.info("Generated SQL query: {}", sqlQuery);

            // Step 4. Execute SQL query
            List<Integer> datacenterIds = executeSqlQuery(sqlQuery);
            logger.info("Found datacenter IDs: {}", datacenterIds);

            // Step 5. Send result to centrala
            String response = sendResultToCentrala(datacenterIds);

            // Step 6. Extract flag from response
            return flagService.findFlagInText(response)
                    .orElse("Task completed. Response: " + response);
        } catch (Exception e) {
            logger.error("Error in w03d03 task: ", e);
            return "Error: " + e.getMessage();
        }
    }

    private List<String> discoverTables() {
        // Step 1.1. Execute SHOW TABLES query
        DatabaseQueryRequestDto requestDto = new DatabaseQueryRequestDto("database", apiKey, "SHOW TABLES");
        String response = apiExplorerService.postJsonForObject(databaseApiUrl, requestDto, String.class);
        
        logger.info("SHOW TABLES response: {}", response);
        
        // Step 1.2. Parse response to extract table names
        return parseTableNames(response);
    }

    private List<String> parseTableNames(String response) {
        // Step 1.2.1. Parse JSON response to extract table names
        List<String> tableNames = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode replyNode = rootNode.get("reply");
            
            if (replyNode != null && replyNode.isArray()) {
                for (JsonNode tableNode : replyNode) {
                    if (tableNode.isObject()) {
                        // Get first field value (table name)
                        JsonNode firstValue = tableNode.fields().next().getValue();
                        if (firstValue != null) {
                            tableNames.add(firstValue.asText());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing table names: ", e);
        }
        
        return tableNames;
    }

    private String getTableSchemas(List<String> tableNames) {
        // Step 2.1. Get schema for each table
        StringBuilder schemas = new StringBuilder();
        
        for (String tableName : tableNames) {
            logger.info("Getting schema for table: {}", tableName);
            
            // Step 2.2. Execute SHOW CREATE TABLE query
            String query = "SHOW CREATE TABLE " + tableName;
            DatabaseQueryRequestDto requestDto = new DatabaseQueryRequestDto("database", apiKey, query);
            String response = apiExplorerService.postJsonForObject(databaseApiUrl, requestDto, String.class);
            
            schemas.append("=== TABLE: ").append(tableName).append(" ===\n");
            schemas.append(response).append("\n\n");
        }
        
        return schemas.toString();
    }

    private String generateSqlQuery(String databaseSchemas) {
        // Step 3.1. Create prompt for SQL generation using PromptService
        String prompt = promptService.w03d03_createSqlQueryPrompt(databaseSchemas);
        
        // Step 3.2. Get SQL query from LLM
        return openAiAdapter.getAnswer(prompt).trim();
    }

    private List<Integer> executeSqlQuery(String sqlQuery) {
        // Step 4.1. Execute the generated SQL query
        DatabaseQueryRequestDto requestDto = new DatabaseQueryRequestDto("database", apiKey, sqlQuery);
        String response = apiExplorerService.postJsonForObject(databaseApiUrl, requestDto, String.class);
        
        logger.info("SQL query execution response: {}", response);
        
        // Step 4.2. Parse response to extract datacenter IDs
        return parseDatacenterIds(response);
    }

    private List<Integer> parseDatacenterIds(String response) {
        // Step 4.2.1. Parse JSON response to extract datacenter IDs
        List<Integer> datacenterIds = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode replyNode = rootNode.get("reply");
            
            if (replyNode != null && replyNode.isArray()) {
                for (JsonNode resultNode : replyNode) {
                    if (resultNode.isObject()) {
                        // Get first field value (datacenter ID)
                        JsonNode firstValue = resultNode.fields().next().getValue();
                        if (firstValue != null) {
                            datacenterIds.add(firstValue.asInt());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing datacenter IDs: ", e);
        }
        
        return datacenterIds;
    }

    private String sendResultToCentrala(List<Integer> datacenterIds) {
        // Step 5.1. Create DTO for API request
        DatabaseReportRequestDto requestDto = new DatabaseReportRequestDto("database", apiKey, datacenterIds);

        logger.info("Sending solution with {} datacenter IDs: {}", datacenterIds.size(), datacenterIds);

        // Step 5.2. Send to API using ApiExplorerService with DTO
        return apiExplorerService.postJsonForObject(reportUrl, requestDto, String.class);
    }
}