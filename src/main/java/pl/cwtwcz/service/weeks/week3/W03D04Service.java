package pl.cwtwcz.service.weeks.week3;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.cwtwcz.service.ApiExplorerService;
import pl.cwtwcz.service.FlagService;
import pl.cwtwcz.adapter.OpenAiAdapter;
import pl.cwtwcz.service.PromptService;
import pl.cwtwcz.dto.week3.LoopApiRequestDto;
import pl.cwtwcz.dto.week3.LoopApiResponseDto;
import pl.cwtwcz.dto.common.ReportRequestDto;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
@Service
public class W03D04Service {

    private static final Logger logger = LoggerFactory.getLogger(W03D04Service.class);

    @Value("${aidevs.api.key}")
    private String apiKey;

    @Value("${custom.report.url}")
    private String reportUrl;

    @Value("${custom.w03d04.barbara-note-url}")
    private String barbaraNoteUrl;

    @Value("${custom.w03d04.people-api-url}")
    private String peopleApiUrl;

    @Value("${custom.w03d04.places-api-url}")
    private String placesApiUrl;

    private final ApiExplorerService apiExplorerService;
    private final FlagService flagService;
    private final OpenAiAdapter openAiAdapter;
    private final PromptService promptService;

    public String w03d04() {
        try {
            logger.info("Starting W03D04 task - finding Barbara Zawadzka");

            // Step 1. Analyze Barbara's note
            String barbaraNote = downloadBarbaraNoteFromUrl();
            NamesAndPlacesData initialData = extractNamesAndPlacesFromNote(barbaraNote);

            // Step 2. Prepare queues for iterative search
            Set<String> peopleQueue = new LinkedHashSet<>(initialData.names);
            Set<String> placesQueue = new LinkedHashSet<>(initialData.places);
            Set<String> processedPeople = new HashSet<>();
            Set<String> processedPlaces = new HashSet<>();
            Set<String> allDiscoveredPeople = new HashSet<>(initialData.names);
            Set<String> allDiscoveredPlaces = new HashSet<>(initialData.places);

            // Step 3. Iterative API querying
            String barbaraLocation = performIterativeSearch(
                    peopleQueue, placesQueue, processedPeople, processedPlaces,
                    allDiscoveredPeople, allDiscoveredPlaces, initialData.places);

            if (barbaraLocation != null) {
                // Step 4. Submit answer to /report
                String response = submitAnswer(barbaraLocation);

                // Step 5. Extract flag if present
                return flagService.findFlagInText(response)
                        .orElse("Barbara found in: " + barbaraLocation + ". Response: " + response);
            } else {
                return "Barbara not found after exhaustive search";
            }

        } catch (Exception e) {
            logger.error("Error in w03d04 task: ", e);
            return "Error: " + e.getMessage();
        }
    }

    private String downloadBarbaraNoteFromUrl() {
        try {
            String note = apiExplorerService.postJsonForObject(barbaraNoteUrl, null, String.class);
            logger.info("Downloaded Barbara's note: {} characters", note.length());
            return note;
        } catch (Exception e) {
            logger.error("Error downloading Barbara's note: ", e);
            throw new RuntimeException("Failed to download Barbara's note", e);
        }
    }

    private NamesAndPlacesData extractNamesAndPlacesFromNote(String note) {
        try {
            String llmResponse = openAiAdapter
                    .getAnswer(promptService.w03d04_createNamesAndPlacesExtractionPrompt(note), "gpt-4.1");
            logger.info("LLM response for name/place extraction: {}", llmResponse);
            return parseNamesAndPlacesFromLLMResponse(llmResponse);

        } catch (Exception e) {
            logger.error("Error using LLM for name/place extraction: ", e);
            throw new RuntimeException("Failed to extract names and places from Barbara's note", e);
        }
    }

    private NamesAndPlacesData parseNamesAndPlacesFromLLMResponse(String llmResponse) {
        Set<String> names = new HashSet<>();
        Set<String> places = new HashSet<>();

        String[] lines = llmResponse.split("\n");
        boolean inNamesSection = false;
        boolean inPlacesSection = false;

        for (String line : lines) {
            line = line.trim();
            if (line.toLowerCase().contains("imiona") || line.toLowerCase().contains("names")) {
                inNamesSection = true;
                inPlacesSection = false;
                continue;
            }
            if (line.toLowerCase().contains("miasta") || line.toLowerCase().contains("places") ||
                    line.toLowerCase().contains("miejsc")) {
                inNamesSection = false;
                inPlacesSection = true;
                continue;
            }

            if (inNamesSection && !line.isEmpty() && !line.startsWith("-") && !line.toLowerCase().contains("imiona")) {
                String name = normalize(line.replaceAll("^[-*•]\\s*", ""));
                if (isValidName(name)) {
                    names.add(name);
                }
            } else if (inPlacesSection && !line.isEmpty() && !line.startsWith("-")
                    && !line.toLowerCase().contains("miasta")) {
                String place = normalize(line.replaceAll("^[-*•]\\s*", ""));
                if (isValidName(place)) {
                    places.add(place);
                }
            }
        }

        logger.info("LLM-extracted names: {}", names);
        logger.info("LLM-extracted places: {}", places);
        return new NamesAndPlacesData(names, places);
    }

    private String performIterativeSearch(Set<String> peopleQueue, Set<String> placesQueue,
            Set<String> processedPeople, Set<String> processedPlaces,
            Set<String> allDiscoveredPeople, Set<String> allDiscoveredPlaces,
            Set<String> initialPlaces) {

        int maxIterations = 50; // Safety limit - increased to allow full search
        int iteration = 0;

        while ((!peopleQueue.isEmpty() || !placesQueue.isEmpty()) && iteration < maxIterations) {
            iteration++;
            logger.info("Starting iteration {}. People queue: {}, Places queue: {}", iteration, peopleQueue,
                    placesQueue);

            // Step 3.1. Process people queue
            if (!peopleQueue.isEmpty()) {
                String person = peopleQueue.iterator().next();
                peopleQueue.remove(person);
                processedPeople.add(person);

                List<String> placesForPerson = queryPeopleAPI(person);

                for (String place : placesForPerson) {
                    logger.info("Processing place {} for person {}. Already discovered: {}, Already processed: {}",
                            place, person, allDiscoveredPlaces.contains(place), processedPlaces.contains(place));
                    if (allDiscoveredPlaces.add(place) && !processedPlaces.contains(place)) {
                        placesQueue.add(place);
                        logger.info("Added new place to queue: {}", place);
                    }
                }
            }

            // Step 3.2. Process places queue
            if (!placesQueue.isEmpty()) {
                String place = placesQueue.iterator().next();
                placesQueue.remove(place);
                processedPlaces.add(place);

                List<String> peopleInPlace = queryPlacesAPI(place);

                if (peopleInPlace.contains("BARBARA") && !initialPlaces.contains(place)) {
                    logger.info("Found Barbara in NEW location: {}", place);
                    return place;
                }

                for (String person : peopleInPlace) {
                    logger.info("Processing person {} from place {}. Already discovered: {}, Already processed: {}",
                            person, place, allDiscoveredPeople.contains(person), processedPeople.contains(person));
                    if (allDiscoveredPeople.add(person) && !processedPeople.contains(person)) {
                        peopleQueue.add(person);
                        logger.info("Added new person to queue: {}", person);
                    }
                }
            }
        }
        logger.warn("Reached max iterations or exhausted all options without finding Barbara");
        return null;
    }

    private List<String> queryPeopleAPI(String name) {
        try {
            LoopApiRequestDto request = new LoopApiRequestDto(apiKey, name);
            LoopApiResponseDto response = apiExplorerService.postJsonForObject(peopleApiUrl, request,
                    LoopApiResponseDto.class);

            logger.info("API response for person {}: {}", name, response);

            if (response.getCode() != 0) {
                logger.warn("API returned error code {} for person {}", response.getCode(), name);
                return new ArrayList<>();
            }

            String message = response.getMessage();
            if (message == null || message.trim().isEmpty()) {
                logger.warn("Empty message for person {}", name);
                return new ArrayList<>();
            }

            String[] rawPlaces = message.split("\\s+");
            logger.info("Raw places for person {}: {}", name, Arrays.toString(rawPlaces));

            List<String> places = Arrays.stream(rawPlaces)
                    .map(this::normalize)
                    .peek(place -> logger.info("Normalized place: '{}' -> valid: {}", place, isValidName(place)))
                    .filter(place -> !place.isEmpty())
                    .filter(this::isValidName)
                    .collect(Collectors.toList());

            logger.info("Person {} was seen in places: {}", name, places);
            return places;
        } catch (Exception e) {
            logger.error("Error querying people API for {}: ", name, e);
            return new ArrayList<>();
        }
    }

    private List<String> queryPlacesAPI(String place) {
        // Step 3.5. Query places API to find people seen in place
        try {
            LoopApiRequestDto request = new LoopApiRequestDto(apiKey, place);
            LoopApiResponseDto response = apiExplorerService.postJsonForObject(placesApiUrl, request,
                    LoopApiResponseDto.class);

            logger.info("API response for place {}: {}", place, response);

            if (response.getCode() != 0) {
                logger.warn("API returned error code {} for place {}", response.getCode(), place);
                return new ArrayList<>();
            }

            String message = response.getMessage();
            if (message == null || message.trim().isEmpty()) {
                logger.warn("Empty message for place {}", place);
                return new ArrayList<>();
            }

            List<String> people = Arrays.asList(message.split("\\s+"));
            logger.info("In place {} were seen people: {}", place, people);
            return people;
        } catch (Exception e) {
            logger.error("Error querying places API for {}: ", place, e);
            return new ArrayList<>();
        }
    }

    private String submitAnswer(String cityName) {
        ReportRequestDto request = new ReportRequestDto("loop", apiKey, cityName);
        logger.info("Submitting answer: {}", cityName);
        return apiExplorerService.postJsonForObject(reportUrl, request, String.class);
    }

    private String normalize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        return StringUtils.stripAccents(text.trim().toUpperCase());
    }

    private boolean isValidName(String name) {
        return StringUtils.isNotBlank(name) && !name.contains("RESTRICTED") && !name.contains("HTTP");
    }

    @AllArgsConstructor
    private static class NamesAndPlacesData {
        final Set<String> names;
        final Set<String> places;
    }
}