package pl.cwtwcz.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextAnalysisService {

    private static final DateTimeFormatter INPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter POLISH_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy",
            Locale.forLanguageTag("pl-PL"));

    /**
     * Extracts structured information from a filename using regex patterns.
     * 
     * @param filename The filename to analyze
     * @return Formatted string with extracted information
     */
    public String extractFilenameInformation(String filename) {
        StringBuilder info = new StringBuilder();

        // Extract date using Java Time API
        String dateInfo = extractDateFromFilename(filename);
        if (!dateInfo.isEmpty()) {
            info.append(dateInfo).append(" ");
        }

        // Extract sector information using regex
        String sectorInfo = extractSectorFromFilename(filename);
        if (!sectorInfo.isEmpty()) {
            info.append(sectorInfo).append(" ");
        }

        // Extract report number
        String reportInfo = extractReportNumberFromFilename(filename);
        if (!reportInfo.isEmpty()) {
            info.append(reportInfo).append(" ");
        }

        return info.toString().trim();
    }

    /**
     * Extracts date information from filename using YYYY-MM-DD pattern and Java
     * Time API.
     * 
     * @param filename The filename to analyze
     * @return Formatted Polish date string or empty if no date found
     */
    private String extractDateFromFilename(String filename) {
        // Pattern for YYYY-MM-DD format (the only format used)
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = datePattern.matcher(filename);
        if (matcher.find()) {
            String dateString = matcher.group(1);
            try {
                LocalDate date = LocalDate.parse(dateString, INPUT_DATE_FORMAT);
                String polishDate = date.format(POLISH_DATE_FORMAT);
                return "Data: " + polishDate + ".";
            } catch (DateTimeParseException e) {
                // Fallback if parsing fails
                return "Data: " + dateString + ".";
            }
        }

        return "";
    }

    /**
     * Extracts sector information from filename.
     * 
     * @param filename The filename to analyze
     * @return Formatted sector string or empty if no sector found
     */
    private String extractSectorFromFilename(String filename) {
        // Pattern for sektor_XX format (where XX can be A1, A3, B2, C1, C2, C4, etc.)
        Pattern sectorPattern = Pattern.compile("sektor_([A-Z]\\d+)");
        Matcher matcher = sectorPattern.matcher(filename);
        if (matcher.find()) {
            return "Sektor " + matcher.group(1) + ".";
        }

        return "";
    }

    /**
     * Extracts report number from filename.
     * 
     * @param filename The filename to analyze
     * @return Formatted report number string or empty if no report number found
     */
    private String extractReportNumberFromFilename(String filename) {
        // Pattern for report-XX- format
        Pattern reportPattern = Pattern.compile("report-(\\d+)-");
        Matcher matcher = reportPattern.matcher(filename);
        if (matcher.find()) {
            return "Raport numer: " + matcher.group(1) + ".";
        }

        return "";
    }

    /**
     * Finds facts related to the report content by matching names and entities.
     * 
     * @param reportContent The content of the report
     * @param facts         Map of fact filenames to their content
     * @return String containing related facts
     */
    public String findRelatedFactsForReport(String reportContent, Map<String, String> facts) {
        StringBuilder relatedFacts = new StringBuilder();
        List<String> possibleNames = extractPossibleNamesFromContent(reportContent);

        for (Map.Entry<String, String> factEntry : facts.entrySet()) {
            String factContent = factEntry.getValue();

            // Check if any names from report match facts
            boolean isRelated = possibleNames.stream()
                    .anyMatch(name -> factContent.toLowerCase().contains(name.toLowerCase()) ||
                            findSimilarNames(name, factContent));

            if (isRelated) {
                relatedFacts.append("Z pliku ").append(factEntry.getKey()).append(":\n");
                relatedFacts.append(factContent).append("\n\n");
            }
        }

        return relatedFacts.toString();
    }

    /**
     * Extracts potential names from content using regex patterns.
     * 
     * @param content The text content to analyze
     * @return List of potential names found
     */
    public List<String> extractPossibleNamesFromContent(String content) {
        List<String> names = new ArrayList<>();
        Pattern namePattern = Pattern.compile("\\b[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+\\b");
        Matcher matcher = namePattern.matcher(content);

        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() > 3) {
                names.add(word);
            }
        }

        return names;
    }

    /**
     * Finds similar names in text, handling potential typos.
     * 
     * @param name The name to search for
     * @param text The text to search in
     * @return true if similar names are found
     */
    public boolean findSimilarNames(String name, String text) {
        String[] textWords = text.split("\\s+");
        return Arrays.stream(textWords)
                .anyMatch(word -> isSimilarName(name, word));
    }

    /**
     * Compares two names for similarity, allowing for minor differences/typos.
     * 
     * @param name1 First name to compare
     * @param name2 Second name to compare
     * @return true if names are similar enough
     */
    public boolean isSimilarName(String name1, String name2) {
        name1 = name1.toLowerCase().replaceAll("[^a-ząćęłńóśźż]", "");
        name2 = name2.toLowerCase().replaceAll("[^a-ząćęłńóśźż]", "");

        if (name1.equals(name2))
            return true;
        if (Math.abs(name1.length() - name2.length()) > 2)
            return false;

        int differences = 0;
        int minLength = Math.min(name1.length(), name2.length());

        for (int i = 0; i < minLength; i++) {
            if (name1.charAt(i) != name2.charAt(i)) {
                differences++;
            }
        }

        differences += Math.abs(name1.length() - name2.length());
        return differences <= 2; // Allow up to 2 character differences
    }

    /**
     * Extracts entities (people, organizations, locations) from text.
     * 
     * @param text The text to analyze
     * @return Map of entity types to lists of found entities
     */
    public Map<String, List<String>> extractEntities(String text) {
        Map<String, List<String>> entities = new HashMap<>();
        entities.put("names", extractPossibleNamesFromContent(text));

        // Add more entity extraction logic as needed
        entities.put("organizations", extractOrganizations(text));
        entities.put("locations", extractLocations(text));

        return entities;
    }

    /**
     * Extracts organization names from text.
     * 
     * @param text The text to analyze
     * @return List of potential organization names
     */
    private List<String> extractOrganizations(String text) {
        List<String> organizations = new ArrayList<>();
        // Simple pattern for organizations (words ending with common org suffixes)
        Pattern orgPattern = Pattern.compile(
                "\\b[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+(?:\\s+[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+)*\\s+(?:Sp\\.|Ltd\\.|Inc\\.|Corp\\.|S\\.A\\.|Sp\\.\\s*z\\s*o\\.o\\.)\\b");
        Matcher matcher = orgPattern.matcher(text);

        while (matcher.find()) {
            organizations.add(matcher.group());
        }

        return organizations;
    }

    /**
     * Extracts location names from text.
     * 
     * @param text The text to analyze
     * @return List of potential location names
     */
    private List<String> extractLocations(String text) {
        List<String> locations = new ArrayList<>();
        // Simple pattern for locations (capitalized words that might be places)
        Pattern locationPattern = Pattern.compile(
                "\\b(?:ul\\.|al\\.|pl\\.)\\s+[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+(?:\\s+[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+)*\\b");
        Matcher matcher = locationPattern.matcher(text);

        while (matcher.find()) {
            locations.add(matcher.group());
        }

        return locations;
    }

    /**
     * Normalizes text by removing special characters and converting to lowercase.
     * 
     * @param text The text to normalize
     * @return Normalized text
     */
    public String normalizeText(String text) {
        if (text == null)
            return "";
        return text.toLowerCase()
                .replaceAll("[^a-ząćęłńóśźż0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Calculates text similarity between two strings.
     * 
     * @param text1 First text
     * @param text2 Second text
     * @return Similarity score between 0.0 and 1.0
     */
    public double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null)
            return 0.0;

        String norm1 = normalizeText(text1);
        String norm2 = normalizeText(text2);

        if (norm1.equals(norm2))
            return 1.0;

        // Simple Levenshtein distance-based similarity
        int maxLength = Math.max(norm1.length(), norm2.length());
        if (maxLength == 0)
            return 1.0;

        int distance = calculateLevenshteinDistance(norm1, norm2);
        return 1.0 - (double) distance / maxLength;
    }

    /**
     * Calculates Levenshtein distance between two strings.
     * 
     * @param str1 First string
     * @param str2 Second string
     * @return Levenshtein distance
     */
    private int calculateLevenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }

        return dp[str1.length()][str2.length()];
    }
}