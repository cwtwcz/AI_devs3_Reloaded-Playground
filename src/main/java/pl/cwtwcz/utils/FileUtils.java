package pl.cwtwcz.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileUtils {
    /**
     * Lists all files in a directory with the given extension (case-insensitive).
     * @param dirPath Directory path
     * @param extension File extension (e.g., ".m4a")
     * @return List of File objects
     */
    public static List<File> listFilesByExtension(String dirPath, String extension) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) return new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(extension.toLowerCase()));
        return files != null ? Arrays.asList(files) : new ArrayList<>();
    }
} 