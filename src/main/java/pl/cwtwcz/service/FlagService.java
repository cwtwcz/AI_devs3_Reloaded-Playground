package pl.cwtwcz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FlagService {
    
    private static final Logger logger = LoggerFactory.getLogger(FlagService.class);
    private static final Pattern FLAG_PATTERN = Pattern.compile("\\{\\{FLG:([^}]*)\\}\\}");
    
    /**
     * Checks if the response contains a flag and logs it if found
     * 
     * @param content The content to check for flags
     * @return Optional containing the flag if found, empty otherwise
     */
    public Optional<String> findFlagInText(String content) {
        if (content == null || content.isEmpty()) {
            return Optional.empty();
        }
        
        Matcher matcher = FLAG_PATTERN.matcher(content);
        if (matcher.find()) {
            logger.info("[IMPORTANT] Found flag in response body: {}", matcher.group());
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }
}
