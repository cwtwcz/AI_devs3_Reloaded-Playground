package pl.cwtwcz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ApiExplorerService {

    private static final Logger logger = LoggerFactory.getLogger(ApiExplorerService.class);
    private final RestTemplate restTemplate;

    public ApiExplorerService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Sends a POST request with a JSON payload to the specified URL and returns the
     * response.
     *
     * @param url          The URL to send the POST request to.
     * @param payload      The object to be serialized to JSON and sent as the
     *                     request body.
     * @param responseType The class of the expected response body.
     * @param <T>          The type of the request payload.
     * @param <R>          The type of the expected response.
     * @return The API response.
     * @throws RuntimeException if there's an error with the API call or response
     *                          processing.
     */
    public <T, R> R postJsonForObject(String url, T payload, Class<R> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.CONTENT_ENCODING, "UTF-8");
        headers.set(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
        HttpEntity<T> requestEntity = new HttpEntity<>(payload, headers);

        try {
            R response = restTemplate.postForObject(url, requestEntity, responseType);
            if (response == null) {
                logger.warn("Received null response from API for URL: {}", url);
                throw new RuntimeException("Received null response from API for URL: " + url);
            }
            return response;
        } catch (Exception e) {
            logger.error("Error during POST API call to {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error during POST API call to " + url + ": " + e.getMessage());
        }
    }

}