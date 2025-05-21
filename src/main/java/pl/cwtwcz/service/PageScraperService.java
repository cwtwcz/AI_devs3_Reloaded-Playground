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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PageScraperService {

    private static final Logger logger = LoggerFactory.getLogger(PageScraperService.class);

    private final String xyzBaseUrl;
    private final String robotUsername;
    private final String robotPassword;
    private final RestTemplate restTemplate;
    private final FlagService flagService;
    private final Pattern URL_PATTERN = Pattern.compile("<a href=\"([^\"]*)\"");

    public PageScraperService(
            RestTemplate restTemplate,
            FlagService flagService,
            @Value("${xyz.base.url}") String xyzBaseUrl,
            @Value("${robot.login.username}") String robotUsername,
            @Value("${robot.login.password}") String robotPassword) {
        this.restTemplate = restTemplate;
        this.flagService = flagService;
        this.xyzBaseUrl = xyzBaseUrl;
        this.robotUsername = robotUsername;
        this.robotPassword = robotPassword;
    }

    public String getQuestionFromLoginPage() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(xyzBaseUrl, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                Matcher pIdMatcher = Pattern.compile("<p id=\"human-question\">Question:<br />(.*?)</p>")
                        .matcher(response.getBody());
                if (pIdMatcher.find()) {
                    String question = pIdMatcher.group(1).trim();
                    logger.info("Found question: {}", question);
                    return question;
                }
            }
            throw new RuntimeException("Could not extract question from login page.");
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error while fetching question from login page", e);
        }
    }

    public String loginToRobotsAndFindFlag(String answer) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", robotUsername);
        map.add("password", robotPassword);
        map.add("answer", answer);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

        try {
            logger.info("Logging into {} with user {}", xyzBaseUrl, robotUsername);
            ResponseEntity<String> response = restTemplate.postForEntity(xyzBaseUrl, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Check if there is a secret link in the response
                Matcher urlMatcher = URL_PATTERN.matcher(response.getBody());
                if (urlMatcher.find()) {
                    String secretPath = urlMatcher.group(1);
                    if (!secretPath.startsWith("http") && !secretPath.startsWith("#")) {
                        URI resolvedUri = URI.create(xyzBaseUrl).resolve(secretPath);
                        logger.info("[IMPORTANT] Found secret link in body: {}", resolvedUri.toString());
                    }
                }

                // Check if flag is in response
                Optional<String> flag = flagService.findFlagInText(response.getBody());
                if (flag.isPresent()) {
                    logger.info("[IMPORTANT] Found flag in response body: {}", flag.get());
                    return flag.get();
                }
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