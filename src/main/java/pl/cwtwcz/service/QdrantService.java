package pl.cwtwcz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class QdrantService {

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${qdrant.api.key}")
    private String qdrantApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Api-Key", qdrantApiKey);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    public void createCollection(String collectionName, int vectorSize) {
        try {
            // Step 1. Create collection with specified vector size
            Map<String, Object> collectionConfig = new HashMap<>();
            Map<String, Object> vectorConfig = new HashMap<>();
            vectorConfig.put("size", vectorSize);
            vectorConfig.put("distance", "Cosine");
            collectionConfig.put("vectors", vectorConfig);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(collectionConfig, createHeaders());
            
            String url = qdrantUrl + "/collections/" + collectionName;
            
            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.PUT, request, String.class);
                log.info("Created Qdrant collection: {} with response: {}", collectionName, response.getStatusCode());
            } catch (Exception e) {
                // Collection might already exist
                log.info("Collection {} might already exist: {}", collectionName, e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Error creating collection: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create collection", e);
        }
    }

    public void indexDocument(String collectionName, String documentId, 
                             List<Float> vector, Map<String, String> metadata) {
        try {
            // Step 2. Create point with vector and metadata
            Map<String, Object> point = new HashMap<>();
            point.put("id", UUID.randomUUID().toString());
            point.put("vector", vector);
            point.put("payload", metadata);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", List.of(point));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, createHeaders());
            
            String url = qdrantUrl + "/collections/" + collectionName + "/points";
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.PUT, request, String.class);
            
            log.debug("Indexed document with ID: {} - Response: {}", documentId, response.getStatusCode());
            
        } catch (Exception e) {
            log.error("Error indexing document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to index document", e);
        }
    }

    public SearchResult searchSimilar(String collectionName, List<Float> queryVector, int limit) {
        try {
            // Step 3. Search for similar vectors
            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("vector", queryVector);
            searchRequest.put("limit", limit);
            searchRequest.put("with_payload", true);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(searchRequest, createHeaders());
            
            String url = qdrantUrl + "/collections/" + collectionName + "/points/search";
            
            ResponseEntity<SearchResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, request, SearchResponse.class);

            SearchResponse searchResponse = response.getBody();
            if (searchResponse != null && searchResponse.result != null && !searchResponse.result.isEmpty()) {
                SearchPoint bestMatch = searchResponse.result.get(0);
                return new SearchResult(bestMatch.score, bestMatch.payload);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error searching: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search", e);
        }
    }

    public static class SearchResponse {
        public List<SearchPoint> result;
    }

    public static class SearchPoint {
        public float score;
        public Map<String, Object> payload;
    }

    public static class SearchResult {
        private final float score;
        private final Map<String, Object> payload;

        public SearchResult(float score, Map<String, Object> payload) {
            this.score = score;
            this.payload = payload;
        }

        public float getScore() {
            return score;
        }

        public String getPayloadValue(String key) {
            Object value = payload.get(key);
            return value != null ? value.toString() : null;
        }
    }
} 