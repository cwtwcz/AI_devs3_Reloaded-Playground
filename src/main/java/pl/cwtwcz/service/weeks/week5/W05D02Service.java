package pl.cwtwcz.service.weeks.week5;

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
import pl.cwtwcz.dto.week3.LoopApiRequestDto;
import pl.cwtwcz.dto.week3.LoopApiResponseDto;
import pl.cwtwcz.dto.week5.GpsRequestDto;
import pl.cwtwcz.dto.week5.GpsLocationDto;
import pl.cwtwcz.dto.week5.GpsApiRequestDto;
import pl.cwtwcz.dto.week5.GpsApiResponseDto;

import static org.apache.commons.lang3.StringUtils.stripAccents;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class W05D02Service {

    private static final Logger logger = LoggerFactory.getLogger(W05D02Service.class);

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${centrala.base.url}")
    private String centralaBaseUrl;

    @Value("${custom.w03d03.database.api.url}")
    private String databaseApiUrl;

    @Value("${custom.w03d04.people-api-url}")
    private String peopleApiUrl;

    @Value("${custom.w03d04.places-api-url}")
    private String placesApiUrl;

    @Value("${centrala.base.url}")
    private String gpsApiUrl;

    private final OpenAiAdapter openAiAdapter;
    private final ApiExplorerService apiExplorerService;
    private final PromptService promptService;
    private final FlagService flagService;
    private final ObjectMapper objectMapper;

    public String w05d02() {
        try {
            logger.info("Starting W05D02 task - GPS tracking agent simulation");

            String agentLogs = downloadAgentLogs();
            String questionData = downloadGpsQuestion();

            // Execute intelligent agent to solve GPS tracking
            Map<String, GpsLocationDto> personLocations = executeGpsAgent(agentLogs, questionData);
            String response = sendGpsResult(personLocations);
            logger.info("Centrala response: {}", response);

            return flagService.findFlagInText(response)
                    .orElse("Task completed. Response: " + response);

        } catch (Exception e) {
            logger.error("Error in w05d02 task: ", e);
            return "Error: " + e.getMessage();
        }
    }

    private Map<String, GpsLocationDto> executeGpsAgent(String agentLogs, String questionData) {
        try {
            logger.info("Starting GPS Agent execution");

            // Step 1: Agent analyzes the problem and creates a plan
            String analysisAndPlan = openAiAdapter.getAnswer(
                    promptService.w05d02_createGpsAgentPrompt(agentLogs, questionData));

            logger.info("Agent Analysis and Plan: {}", analysisAndPlan);

            // Step 2: Execute the plan step by step
            return executeAgentPlan(analysisAndPlan);

        } catch (Exception e) {
            logger.error("Error in GPS agent execution: ", e);
            return new HashMap<>();
        }
    }

    private Map<String, GpsLocationDto> executeAgentPlan(String planText) {
        Map<String, GpsLocationDto> personLocations = new HashMap<>();

        try {
            logger.info("Executing dynamic agent plan based on LLM analysis");

            // Step 1: Parse the plan to extract actionable steps
            List<PlanStep> steps = parsePlanSteps(planText);
            logger.info("Parsed {} steps from LLM plan", steps.size());

            // Step 2: Execute steps dynamically
            Map<String, Object> executionContext = new HashMap<>();
            executionContext.put("personLocations", personLocations);

            for (PlanStep step : steps) {
                logger.info("Executing Step {}: {} - Tool: {}", step.number, step.description, step.tool);
                executeStep(step, executionContext);
            }

            // Step 3: Return final results
            return (Map<String, GpsLocationDto>) executionContext.get("personLocations");

        } catch (Exception e) {
            logger.error("Error executing agent plan: ", e);
            return personLocations;
        }
    }

    private void executeStep(PlanStep step, Map<String, Object> context) {
        try {
            String tool = step.tool.toUpperCase();
            String description = step.description.toLowerCase();

            if (tool.contains("PLACES_API")) {
                executeStepPlacesApi(step, context);
            } else if (tool.contains("PEOPLE_API")) {
                executeStepPeopleApi(step, context);
            } else if (tool.contains("DB_EXECUTE_SQL")) {
                executeStepDatabase(step, context);
            } else if (tool.contains("DB_DISCOVER_TABLES")) {
                executeStepDiscoverDatabaseTables(step, context);
            } else if (tool.contains("DB_DISCOVER_TABLE_SCHEMAS")) {
                executeStepDiscoverTableSchemas(step, context);
            } else if (description.contains("filtr")) {
                executeStepFilterBarbara(step, context);
            } else if (description.contains("tłumacz") || description.contains("polskich znaków")) {
                executeStepNormalizeNames(step, context);
            } else if (tool.contains("GPS_API")) {
                executeStepGpsLookup(step, context);
            } else {
                logger.info("Step {} - Unknown step type, executing generic handler", step.number);
            }

        } catch (Exception e) {
            logger.error("Error executing step {}: {}", step.number, e.getMessage(), e);
        }
    }

    private String downloadAgentLogs() {
        String logsUrl = centralaBaseUrl + "data/" + apiKey + "/gps.txt";
        logger.info("Downloading agent logs from: {}", logsUrl);
        return apiExplorerService.postJsonForObject(logsUrl, null, String.class);
    }

    private String downloadGpsQuestion() {
        String questionUrl = centralaBaseUrl + "data/" + apiKey + "/gps_question.json";
        logger.info("Downloading GPS question from: {}", questionUrl);
        return apiExplorerService.postJsonForObject(questionUrl, null, String.class);
    }

    /**
     * Tool: Query Database - check which tables are available.
     * 
     * @param query
     * @return
     */
    private List<String> tool_discoverDatabaseTables() {
        DatabaseQueryRequestDto requestDto = new DatabaseQueryRequestDto("database", apiKey, "SHOW TABLES");
        String response = apiExplorerService.postJsonForObject(databaseApiUrl, requestDto, String.class);
        logger.info("SHOW TABLES response: {}", response);
        return parseTableNames(response);
    }

    /**
     * Tool:Query people API to find places where person was seen.
     * 
     * @param name
     * @return
     */
    private List<String> tool_queryPeopleApi(String name) {
        try {
            String normalizedName = stripAccents(name.trim().toUpperCase());
            LoopApiRequestDto request = new LoopApiRequestDto(apiKey, normalizedName);
            LoopApiResponseDto response = apiExplorerService.postJsonForObject(peopleApiUrl, request,
                    LoopApiResponseDto.class);

            if (response.getCode() != 0 || response.getMessage() == null || response.getMessage().trim().isEmpty()) {
                logger.warn("People API returned code {} for person {}", response.getCode(), name);
                return new ArrayList<>();
            }

            List<String> places = Arrays.asList(response.getMessage().split("\\s+"));
            logger.info("Person {} was seen in places: {}", name, places);
            return places;

        } catch (Exception e) {
            logger.error("Error querying people API for {}: ", name, e);
            return new ArrayList<>();
        }
    }

    /**
     * Tool: Query places API to find people seen in place.
     * 
     * @param place
     * @return
     */
    private List<String> tool_queryPlacesApi(String place) {
        try {
            LoopApiRequestDto request = new LoopApiRequestDto(apiKey, place);
            LoopApiResponseDto response = apiExplorerService.postJsonForObject(placesApiUrl, request,
                    LoopApiResponseDto.class);

            if (response.getCode() != 0 || response.getMessage() == null || response.getMessage().trim().isEmpty()) {
                logger.warn("Places API returned code {} for place {}", response.getCode(), place);
                return new ArrayList<>();
            }

            List<String> people = Arrays.asList(response.getMessage().split("\\s+"));
            logger.info("In place {} were seen people: {}", place, people);
            return people;
        } catch (Exception e) {
            logger.error("Error querying places API for {}: ", place, e);
            return new ArrayList<>();
        }
    }

    private String tool_executeSqlQuery(String sqlQuery) {
        DatabaseQueryRequestDto requestDto = new DatabaseQueryRequestDto("database", apiKey, sqlQuery);
        String response = apiExplorerService.postJsonForObject(databaseApiUrl, requestDto, String.class);
        logger.info("SQL query execution response: {}", response);
        return response;
    }

    private String tool_discoverTableSchemas(List<String> tableNames) {
        StringBuilder schemas = new StringBuilder();

        for (String tableName : tableNames) {
            logger.info("Getting schema for table: {}", tableName);
            DatabaseQueryRequestDto requestDto = new DatabaseQueryRequestDto("database", apiKey,
                    "SHOW CREATE TABLE " + tableName);
            String response = apiExplorerService.postJsonForObject(databaseApiUrl, requestDto, String.class);
            schemas.append("=== TABLE: ").append(tableName).append(" ===\n");
            schemas.append(response).append("\n\n");
        }

        return schemas.toString();
    }

    private List<String> parseTableNames(String response) {
        List<String> tableNames = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode replyNode = rootNode.get("reply");

            if (replyNode != null && replyNode.isArray()) {
                for (JsonNode tableNode : replyNode) {
                    if (tableNode.isObject()) {
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

    private List<PlanStep> parsePlanSteps(String planText) {
        List<PlanStep> steps = new ArrayList<>();

        // Find the PLAN DZIAŁANIA section
        String[] sections = planText.split("PLAN DZIAŁANIA:");
        if (sections.length < 2) {
            logger.warn("No PLAN DZIAŁANIA section found in LLM response");
            return steps;
        }

        // Get text after PLAN DZIAŁANIA until next section
        String planSection = sections[1];
        String[] nextSections = planSection.split("STRATEGIA ROZWIĄZANIA:");
        if (nextSections.length > 0) {
            planSection = nextSections[0];
        }

        // Parse individual steps
        String[] lines = planSection.split("\n");
        for (String line : lines) {
            line = line.trim();

            // Look for step pattern: "Krok X: [description] - Narzędzie: [tool]"
            if (line.matches("^Krok \\d+:.*")) {
                PlanStep step = parseStepLine(line);
                if (step != null) {
                    steps.add(step);
                }
            }
        }
        return steps;
    }

    private PlanStep parseStepLine(String line) {
        try {
            // Extract step number
            String numberPart = line.substring(line.indexOf("Krok ") + 5, line.indexOf(":"));
            int stepNumber = Integer.parseInt(numberPart.trim());

            // Extract description and tool
            String rest = line.substring(line.indexOf(":") + 1).trim();
            String[] parts = rest.split(" - Narzędzie:");

            String description = parts[0].trim();
            String tool = parts.length > 1 ? parts[1].trim() : "MANUAL";

            // Clean up tool string - remove square brackets if present
            tool = tool.replaceAll("^\\[|\\]$", "").trim();

            return new PlanStep(stepNumber, description, tool);

        } catch (Exception e) {
            logger.warn("Failed to parse step line: {} - {}", line, e.getMessage());
            return null;
        }
    }

    private void executeStepPlacesApi(PlanStep step, Map<String, Object> context) {
        logger.info("Executing PLACES_API step: {}", step.description);

        // Extract place parameter from tool field (format: "PLACES_API:LUBAWA")
        String place = null;
        if (step.tool.contains(":")) {
            String[] toolParts = step.tool.split(":");
            if (toolParts.length >= 2) {
                place = toolParts[1].trim().toUpperCase();
                // Clean up parameter - remove any remaining brackets
                place = place.replaceAll("\\[|\\]", "").trim();
            }
        }

        // Fallback: extract from description if no parameter in tool
        if (place == null) {
            if (step.description.contains("\"")) {
                String[] parts = step.description.split("\"");
                if (parts.length >= 2) {
                    place = parts[1].toUpperCase();
                }
            } else {
                logger.warn("No place parameter found in tool '{}' or description '{}'", step.tool, step.description);
                return;
            }
        }

        List<String> peopleInPlace = tool_queryPlacesApi(place);
        context.put("peopleInPlace", peopleInPlace);
        context.put("targetPlace", place);

        logger.info("Found {} people in place {}: {}", peopleInPlace.size(), place, peopleInPlace);
    }

    private void executeStepPeopleApi(PlanStep step, Map<String, Object> context) {
        logger.info("Executing PEOPLE_API step: {}", step.description);

        // Extract person parameter from tool field (format: "PEOPLE_API:RAFAL")
        String personName = null;
        if (step.tool.contains(":")) {
            String[] toolParts = step.tool.split(":");
            if (toolParts.length >= 2) {
                personName = toolParts[1].trim().toUpperCase();
                // Clean up parameter - remove any remaining brackets
                personName = personName.replaceAll("\\[|\\]", "").trim();
            }
        }

        // Fallback: extract from description if no parameter in tool
        if (personName == null) {
            if (step.description.contains("\"")) {
                String[] parts = step.description.split("\"");
                if (parts.length >= 2) {
                    personName = parts[1].toUpperCase();
                }
            } else {
                logger.warn("No person parameter found in tool '{}' or description '{}'", step.tool, step.description);
                return;
            }
        }

        List<String> placesForPerson = tool_queryPeopleApi(personName);
        context.put("placesForPerson", placesForPerson);
        context.put("targetPerson", personName);

        logger.info("Found {} places for person {}: {}", placesForPerson.size(), personName, placesForPerson);
    }

    // Improve: make more generic - LLM could generate the SQL query
    private void executeStepDatabase(PlanStep step, Map<String, Object> context) {
        logger.info("Executing DATABASE step: {}", step.description);

        @SuppressWarnings("unchecked")
        List<String> peopleToCheck = (List<String>) context.get("filteredPeople");
        if (peopleToCheck == null) {
            peopleToCheck = (List<String>) context.get("peopleInPlace");
        }

        if (peopleToCheck != null) {
            Map<String, Integer> userIds = new HashMap<>();

            for (String person : peopleToCheck) {
                String normalizedName = stripAccents(person.trim().toUpperCase());
                String query = String.format("SELECT id FROM users WHERE username = '%s'", normalizedName);

                try {
                    String response = tool_executeSqlQuery(query);
                    Integer userId = parseUserIdFromResponse(response);
                    if (userId != null) {
                        userIds.put(person, userId);
                        logger.info("Found userID {} for person {}", userId, person);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to get userID for {}: {}", person, e.getMessage());
                }
            }

            context.put("userIds", userIds);
        }
    }

    private void executeStepFilterBarbara(PlanStep step, Map<String, Object> context) {
        logger.info("Executing FILTER BARBARA step: {}", step.description);

        @SuppressWarnings("unchecked")
        List<String> peopleInPlace = (List<String>) context.get("peopleInPlace");
        if (peopleInPlace != null) {
            List<String> filteredPeople = peopleInPlace.stream()
                    .filter(person -> !person.toUpperCase().contains("BARBARA"))
                    .collect(java.util.stream.Collectors.toList());

            context.put("filteredPeople", filteredPeople);
            logger.info("Filtered out Barbara. Remaining people: {}", filteredPeople);
        }
    }

    private void executeStepNormalizeNames(PlanStep step, Map<String, Object> context) {
        logger.info("Executing NORMALIZE NAMES step: {}", step.description);

        // Names are already normalized in our tool methods using stripAccents
        // This step is acknowledgment that normalization is handled
        logger.info("Name normalization is handled automatically in API calls");
    }

    private void executeStepGpsLookup(PlanStep step, Map<String, Object> context) {
        logger.info("Executing GPS LOOKUP step: {}", step.description);

        @SuppressWarnings("unchecked")
        Map<String, Integer> userIds = (Map<String, Integer>) context.get("userIds");
        @SuppressWarnings("unchecked")
        Map<String, GpsLocationDto> personLocations = (Map<String, GpsLocationDto>) context.get("personLocations");

        if (userIds != null && personLocations != null) {
            for (Map.Entry<String, Integer> entry : userIds.entrySet()) {
                String personName = entry.getKey();
                Integer userId = entry.getValue();

                try {
                    // According to logs, we need to query GPS endpoint with userID
                    GpsLocationDto location = tool_queryGpsApi(userId);
                    if (location != null) {
                        personLocations.put(personName, location);
                        logger.info("Got GPS coordinates for {}: {}", personName, location);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to get GPS for {} (userID: {}): {}", personName, userId, e.getMessage());
                }
            }
        }
    }

    private Integer parseUserIdFromResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode replyNode = rootNode.get("reply");

            if (replyNode != null && replyNode.isArray() && replyNode.size() > 0) {
                JsonNode firstResult = replyNode.get(0);
                if (firstResult.isObject() && firstResult.has("id")) {
                    return firstResult.get("id").asInt();
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing userID from response: ", e);
        }
        return null;
    }

    /**
     * Tool: Query GPS API to get coordinates for a specific userID.
     * 
     * @param userId
     * @return
     */
    private GpsLocationDto tool_queryGpsApi(Integer userId) {
        try {
            logger.info("Querying GPS endpoint for userID: {}", userId);

            // Create GPS API request with userID
            GpsApiRequestDto gpsRequest = new GpsApiRequestDto(userId);

            // Make HTTP POST request to GPS endpoint
            String gpsUrl = gpsApiUrl + "gps";
            GpsApiResponseDto response = apiExplorerService.postJsonForObject(gpsUrl, gpsRequest,
                    GpsApiResponseDto.class);

            logger.info("GPS API response for userID {}: {}", userId, response);

            // Check if response is successful
            if (response.getCode() == 0 && response.getMessage() != null) {
                logger.info("Successfully got GPS coordinates for userID {}: {}", userId, response.getMessage());
                return response.getMessage();
            } else {
                logger.warn("GPS API returned error code {} for userID {}", response.getCode(), userId);
                return null;
            }

        } catch (Exception e) {
            logger.error("Error querying GPS endpoint for userID {}: ", userId, e);
            return null;
        }
    }

    // Helper class for plan steps
    private static class PlanStep {
        final int number;
        final String description;
        final String tool;

        PlanStep(int number, String description, String tool) {
            this.number = number;
            this.description = description;
            this.tool = tool;
        }
    }

    private String sendGpsResult(Map<String, GpsLocationDto> personLocations) {
        // Step 6.1. Create request DTO
        GpsRequestDto requestDto = new GpsRequestDto("gps", apiKey, personLocations);

        logger.info("Sending GPS result with {} locations", personLocations.size());

        // Step 6.2. Send to centrala
        return apiExplorerService.postJsonForObject(reportUrl, requestDto, String.class);
    }

    private void executeStepDiscoverDatabaseTables(PlanStep step, Map<String, Object> context) {
        logger.info("Executing DISCOVER DATABASE TABLES step: {}", step.description);

        List<String> tables = tool_discoverDatabaseTables();
        context.put("tables", tables);
        logger.info("Found {} tables: {}", tables.size(), tables);
    }

    private void executeStepDiscoverTableSchemas(PlanStep step, Map<String, Object> context) {
        logger.info("Executing DISCOVER DATABASE TABLE SCHEMAS step: {}", step.description);

        List<String> tables = (List<String>) context.get("tables");
        String schemas = tool_discoverTableSchemas(tables);
        context.put("schemas", schemas);
        logger.info("Found schemas: {}", schemas);
    }
}
