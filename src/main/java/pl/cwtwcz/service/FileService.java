package pl.cwtwcz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final ObjectMapper objectMapper;

    public FileService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Reads a JSON file from the specified path and maps it to a Java object.
     *
     * @param filePath  The path to the JSON file (e.g.,
     *                  "src/main/resources/input.json").
     * @param valueType The class of the object to map the JSON to.
     * @param <T>       The type of the object.
     * @return The mapped Java object.
     */
    public <T> T readJsonFileToObject(String filePath, Class<T> valueType) {
        logger.info("Attempting to read and parse JSON file: {}", filePath);
        File inputFile = Paths.get(filePath).toFile();
        try {
            if (!inputFile.exists()) {
                throw new IOException("Input file not found: " + filePath);
            }

            T result = objectMapper.readValue(inputFile, valueType);
            logger.info("Successfully read and parsed JSON file: {}", filePath);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read or parse JSON file " + filePath + ": " + e.getMessage(), e);
        }
    }
}