package pl.cwtwcz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.cwtwcz.dto.week1.day2.VerifyRequestDto;
import pl.cwtwcz.dto.week1.day2.VerifyResponseDto;

@Service
public class ApiExplorerService {

    private static final Logger logger = LoggerFactory.getLogger(ApiExplorerService.class);
    private final RestTemplate restTemplate;
    private final String verifyApiUrl;

    public ApiExplorerService(
            RestTemplate restTemplate,
            @Value("${api.verify.url}") String verifyApiUrl) {
        this.restTemplate = restTemplate;
        this.verifyApiUrl = verifyApiUrl;
    }

    /**
     * Makes a verification call to the API for week 1 day 2 task.
     *
     * @param text The text to send in the request
     * @param msgId The message ID for the request
     * @return The API response
     * @throws RuntimeException if there's an error with the API call or response processing
     */
    public VerifyResponseDto w01d02verifyCall(String text, String msgId) {
        VerifyRequestDto requestDto = new VerifyRequestDto(text, msgId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<VerifyRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);

        try {
            VerifyResponseDto response = restTemplate.postForObject(verifyApiUrl, requestEntity, VerifyResponseDto.class);
            if (response == null) {
                logger.warn("Received null response from API.");
                throw new RuntimeException("Received null response from API.");
            }
            return response;
        } catch (Exception e) {
            logger.error("Error during API call: {}", e.getMessage(), e);
            throw new RuntimeException("Error during API call: " + e.getMessage());
        }
    }
} 