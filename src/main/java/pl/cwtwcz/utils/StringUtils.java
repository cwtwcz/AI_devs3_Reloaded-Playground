package pl.cwtwcz.utils;

/**
 * Utility class for string operations.
 * 
 * Utils created to prevent dependencies for only a few simple methods.
 */
public class StringUtils {
    /**
     * Checks if a string is empty.
     * 
     * @param str The string to check.
     * @return true if the string is empty, false otherwise.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if a string is not empty.
     * 
     * @param str The string to check.
     * @return true if the string is not empty, false otherwise.
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}