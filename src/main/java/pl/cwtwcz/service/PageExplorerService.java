package pl.cwtwcz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PageExplorerService {

    private static final Logger logger = LoggerFactory.getLogger(PageExplorerService.class);

    @Value("${xyz.base.url}")
    private String xyzBaseUrl;

    @Value("${centrala.base.url}")
    private String centralaBaseUrl;

    @Value("${robot.login.username}")
    private String robotUsername;

    @Value("${robot.login.password}")
    private String robotPassword;

    private final RestTemplate restTemplate;
    private final FlagService flagService;

    private Pattern URL_PATTERN = Pattern.compile("<a href=\"([^\"]*)\"");

    public PageExplorerService(RestTemplate restTemplate, FlagService flagService) {
        this.restTemplate = restTemplate;
        this.flagService = flagService;
    }

    public String getQuestionFromLoginPage() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(xyzBaseUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                Matcher pIdMatcher = Pattern.compile("<p id=\"human-question\">Question:<br />(.*?)</p>")
                        .matcher(response.getBody());
                if (pIdMatcher.find()) {
                    String question = pIdMatcher.group(1).trim();
                    logger.debug("Found question: {}", question);
                    return question;
                }
            }
            throw new RuntimeException("Could not extract question from login page.");
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while fetching question from login page", e);
        }
    }

    public String loginToRobots(String answer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", robotUsername);
        map.add("password", robotPassword);
        map.add("answer", answer);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

        try {
            logger.debug("Logging into {} with user {}", xyzBaseUrl, robotUsername);
            ResponseEntity<String> response = restTemplate.postForEntity(xyzBaseUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                URI location = response.getHeaders().getLocation();
                if (location != null) {
                    logger.info("Login successful, redirecting to: {}", location.toString());
                    return location.toString();
                }
                String responseBody = response.getBody();
                if (responseBody != null) {
                    flagService.checkIfFlagInText(responseBody);

                    Matcher urlMatcher = URL_PATTERN.matcher(responseBody);
                    if (urlMatcher.find()) {
                        String secretPath = urlMatcher.group(1);
                        if (!secretPath.startsWith("http") && !secretPath.startsWith("#")) {
                            URI baseUri = URI.create(xyzBaseUrl);
                            URI resolvedUri = baseUri.resolve(secretPath);
                            logger.info("Login successful, found link in body: {}", resolvedUri.toString());
                            return resolvedUri.toString();
                        }
                    }
                }
                logger.warn("Login returned 2xx status, but no redirect URL or link in body found.");
            }
            String errorMessage = String.format("Login failed. Status code: %s. Response: %s",
                    response.getStatusCode(),
                    response.getBody() != null ? response.getBody() : "[EMPTY RESPONSE]");
            throw new RuntimeException(errorMessage);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during login", e);
        }
    }
}