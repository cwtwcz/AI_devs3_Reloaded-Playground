package pl.cwtwcz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import java.util.Map;
import java.util.HashMap;

@RequiredArgsConstructor
@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final ObjectMapper objectMapper;

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

    /**
     * Reads a file from the specified path and returns its contents as a
     * base64-encoded string.
     * 
     * @param filePath The path to the file.
     * @return The base64-encoded contents of the file.
     */
    public String readFileAsBase64(String filePath) {
        logger.info("Reading file as base64: {}", filePath);
        try {
            Path path = Paths.get(filePath);
            byte[] fileBytes = Files.readAllBytes(path);
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file as base64: " + filePath, e);
        }
    }

    /**
     * Writes string content to a file.
     * 
     * @param filePath The path where to write the file.
     * @param content The string content to write.
     */
    public void writeStringToFile(String filePath, String content) {
        logger.info("Writing string content to file: {}", filePath);
        try {
            Files.write(Paths.get(filePath), content.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to write string to file: " + filePath, e);
        }
    }

    /**
     * Reads a text file and returns its content as a string.
     * 
     * @param filePath The path to the file to read.
     * @return The file content as a string.
     */
    public String readStringFromFile(String filePath) {
        logger.info("Reading string content from file: {}", filePath);
        try {
            return Files.readString(Paths.get(filePath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read string from file: " + filePath, e);
        }
    }

    /**
     * Checks if a file exists at the specified path.
     * 
     * @param filePath The path to check.
     * @return True if the file exists, false otherwise.
     */
    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Creates directories for the specified path if they don't exist.
     * 
     * @param directoryPath The directory path to create.
     */
    public void createDirectories(String directoryPath) {
        logger.info("Creating directories: {}", directoryPath);
        try {
            Files.createDirectories(Paths.get(directoryPath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create directories: " + directoryPath, e);
        }
    }

    /**
     * Downloads a file from URL to the specified destination path.
     * 
     * @param url The URL to download from.
     * @param destinationPath The path where to save the downloaded file.
     */
    public void downloadFile(String url, String destinationPath) {
        logger.info("Downloading file from {} to {}", url, destinationPath);
        try {
            URL fileUrl = URI.create(url).toURL();
            try (InputStream in = fileUrl.openStream();
                    FileOutputStream out = new FileOutputStream(destinationPath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            logger.info("Successfully downloaded file: {}", destinationPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + url, e);
        }
    }

    /**
     * Lists all files in a directory that match the given filter criteria.
     * 
     * @param directoryPath The directory path to search in.
     * @param fileExtension The file extension to filter by (e.g., ".txt").
     * @param nameContains Optional string that filename must contain (can be null).
     * @return Map of filename to file path for matching files.
     */
    public Map<String, String> listFilesInDirectory(String directoryPath, String fileExtension, String nameContains) {
        logger.info("Listing files in directory: {} with extension: {} containing: {}", directoryPath, fileExtension, nameContains);
        Map<String, String> files = new HashMap<>();
        try {
            File dir = new File(directoryPath);
            if (!dir.exists() || !dir.isDirectory()) {
                logger.warn("Directory does not exist or is not a directory: {}", directoryPath);
                return files;
            }

            File[] fileArray = dir.listFiles((file, name) -> {
                boolean matchesExtension = name.endsWith(fileExtension);
                boolean matchesContent = nameContains == null || name.contains(nameContains);
                return matchesExtension && matchesContent;
            });

            if (fileArray != null) {
                for (File file : fileArray) {
                    files.put(file.getName(), file.getAbsolutePath());
                }
            }
            
            logger.info("Found {} matching files", files.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to list files in directory: " + directoryPath, e);
        }
        return files;
    }

    /**
     * Reads multiple files and returns their contents as a map.
     * 
     * @param filePaths Map of filename to file path.
     * @return Map of filename to file content.
     */
    public Map<String, String> readMultipleFiles(Map<String, String> filePaths) {
        logger.info("Reading {} files", filePaths.size());
        Map<String, String> fileContents = new HashMap<>();
        
        for (Map.Entry<String, String> entry : filePaths.entrySet()) {
            String filename = entry.getKey();
            String filePath = entry.getValue();
            try {
                String content = readStringFromFile(filePath);
                fileContents.put(filename, content);
                logger.info("Successfully read file: {}", filename);
            } catch (Exception e) {
                logger.error("Failed to read file: {} at path: {}", filename, filePath, e);
                // Continue reading other files even if one fails
            }
        }
        
        return fileContents;
    }
}