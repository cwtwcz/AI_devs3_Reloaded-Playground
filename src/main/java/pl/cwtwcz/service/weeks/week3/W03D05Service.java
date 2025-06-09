package pl.cwtwcz.service.weeks.week3;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.dto.week3.DatabaseQueryRequestDto;
import pl.cwtwcz.dto.week3.DatabaseResponseDto;
import pl.cwtwcz.dto.week3.UserDto;
import pl.cwtwcz.dto.week3.ConnectionDto;
import pl.cwtwcz.dto.common.ReportRequestDto;
import pl.cwtwcz.entity.User;
import pl.cwtwcz.repository.UserRepository;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;

@RequiredArgsConstructor
@Service
public class W03D05Service {

    private static final Logger logger = LoggerFactory.getLogger(W03D05Service.class);

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${custom.w03d03.database.api.url}")
    private String databaseApiUrl;

    private final ApiExplorerService apiExplorerService;
    private final FlagService flagService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public String w03d05() {
        try {
            logger.info("Starting W03D05 task - finding shortest path from Rafał to Barbara");

            // Step 1. Fetch data from MySQL database
            List<UserDto> users = fetchUsersFromDatabase();
            List<ConnectionDto> connections = fetchConnectionsFromDatabase();
            logger.info("Fetched {} users and {} connections from database", users.size(), connections.size());

            // Step 2. Try Neo4j first, fallback to in-memory if needed
            List<String> shortestPath = null;
            try {
                clearNeo4jDatabase();
                loadDataIntoNeo4j(users, connections);
                shortestPath = findShortestPath("Rafał", "Barbara");
                logger.info("Used Neo4j for shortest path calculation");
            } catch (Exception e) {
                logger.warn("Neo4j not available: {}", e.getMessage());
                Throwables.propagateIfPossible(e, RuntimeException.class);
            }

            if (isEmpty(shortestPath)) {
                return "No path found from Rafał to Barbara";
            }

            // Step 3. Convert path to comma-separated string
            String pathAnswer = String.join(",", shortestPath);
            logger.info("Found shortest path: {}", pathAnswer);

            // Step 4. Submit answer to /report
            String response = submitAnswer(pathAnswer);

            // Step 5. Extract flag if present
            return flagService.findFlagInText(response)
                    .orElse("Path found: " + pathAnswer + ". Response: " + response);

        } catch (Exception e) {
            logger.error("Error in w03d05 task: ", e);
            return "Error: " + e.getMessage();
        }
    }

    private List<UserDto> fetchUsersFromDatabase() {
        DatabaseQueryRequestDto requestDto = new DatabaseQueryRequestDto("database", apiKey,
                "SELECT id, username FROM users");
        String response = apiExplorerService.postJsonForObject(databaseApiUrl, requestDto, String.class);
        try {
            DatabaseResponseDto<UserDto> databaseResponse = objectMapper.readValue(response, objectMapper
                    .getTypeFactory().constructParametricType(DatabaseResponseDto.class, UserDto.class));
            return databaseResponse.getReply() != null ? databaseResponse.getReply() : new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error parsing users from response: ", e);
            return new ArrayList<>();
        }

    }

    private List<ConnectionDto> fetchConnectionsFromDatabase() {
        DatabaseQueryRequestDto requestDto = new DatabaseQueryRequestDto("database", apiKey,
                "SELECT user1_id, user2_id FROM connections");
        String response = apiExplorerService.postJsonForObject(databaseApiUrl, requestDto, String.class);
        logger.info("Connections query response: {}", response);

        try {
            DatabaseResponseDto<ConnectionDto> databaseResponse = objectMapper.readValue(response, objectMapper
                    .getTypeFactory().constructParametricType(DatabaseResponseDto.class, ConnectionDto.class));

            return databaseResponse.getReply() != null ? databaseResponse.getReply() : new ArrayList<>();
        } catch (Exception e) {
            logger.error("Error parsing connections from response: ", e);
            return new ArrayList<>();
        }
    }

    private void clearNeo4jDatabase() {
        try {
            // DETACH DELETE which automatically removes relationships before deleting nodes
            userRepository.deleteAllUserAndRelationships();
            logger.info("Cleared existing Neo4j data using DETACH DELETE");
        } catch (Exception e) {
            logger.warn("Error clearing Neo4j data (database might be empty): ", e);
        }
    }

    private void loadDataIntoNeo4j(List<UserDto> users, List<ConnectionDto> connections) {
        // Step 2.1. Filter out users with null IDs
        List<UserDto> validUsers = users.stream()
                .filter(user -> user.getId() != null && user.getUsername() != null)
                .toList();

        // Step 2.2. Create user nodes first - save them all at once
        Map<Integer, User> userMap = new HashMap<>();
        for (UserDto userDto : validUsers) {
            User user = new User();
            user.setUserId(userDto.getId());
            user.setUsername(userDto.getUsername());
            // Don't set the Neo4j ID - let it be auto-generated
            userMap.put(userDto.getId(), user);
            logger.debug("Prepared user node: {} (Business ID: {})", userDto.getUsername(), userDto.getId());
        }

        // Step 2.3. Filter connections and build relationships before saving
        List<ConnectionDto> validConnections = connections.stream()
                .filter(conn -> conn.getUser1_id() != null && conn.getUser2_id() != null)
                .filter(conn -> userMap.containsKey(conn.getUser1_id()) && userMap.containsKey(conn.getUser2_id()))
                .toList();

        for (ConnectionDto connectionDto : validConnections) {
            User user1 = userMap.get(connectionDto.getUser1_id());
            User user2 = userMap.get(connectionDto.getUser2_id());

            if (user1 != null && user2 != null) {
                user1.getConnections().add(user2);
                user2.getConnections().add(user1);
                logger.debug("Created connection: {} <-> {}", user1.getUsername(), user2.getUsername());
            }
        }

        // Step 2.4. Save all users with their relationships in one operation
        userRepository.saveAll(userMap.values());
        logger.info("Loaded {} users and {} connections into Neo4j", validUsers.size(), validConnections.size());
    }

    private List<String> findShortestPath(String startUsername, String endUsername) {
        try {
            List<String> path = userRepository.findShortestPath(startUsername, endUsername);
            logger.info("Shortest path from {} to {}: {}", startUsername, endUsername, path);
            return path;
        } catch (Exception e) {
            logger.error("Error finding shortest path: ", e);
            return null;
        }
    }

    private String submitAnswer(String pathAnswer) {
        ReportRequestDto request = new ReportRequestDto("connections", apiKey, pathAnswer);
        logger.info("Submitting answer: {}", pathAnswer);
        return apiExplorerService.postJsonForObject(reportUrl, request, String.class);
    }
}