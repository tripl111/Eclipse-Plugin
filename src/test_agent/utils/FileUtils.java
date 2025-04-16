package test_agent.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for file and path related operations.
 */
public final class FileUtils {
    private static final Logger logger = Logger.getLogger(FileUtils.class.getName());

    private FileUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Reads the content of a file into a String.
     * Logs an error and returns a descriptive error message string if reading fails.
     * NOTE: Returning an error message string can be brittle. Consider throwing exceptions in future refactoring.
     *
     * @param filePath The path to the file.
     * @return The file content as a String, or an error message string starting with "Error reading ".
     */
    public static String readFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warning("readFile called with null or empty filePath.");
            return "Error reading null or empty file path";
        }
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException | SecurityException | OutOfMemoryError e) {
            String errorMessage = "Error reading " + filePath + ": " + e.getMessage();
            logger.log(Level.WARNING, errorMessage, e);
            return errorMessage;
        }
    }

    /**
     * Calculates the relative path from a base path to a full path.
     * Handles potential errors during relativization (e.g., different drives on Windows).
     *
     * @param fullPath The full path to a file or directory.
     * @param basePath The base directory path.
     * @return The relative path as a String, or the original fullPath if relativization fails.
     * @throws NullPointerException if either path is null.
     */
    public static String getRelativePath(String fullPath, String basePath) {
        if (fullPath == null || basePath == null) {
            throw new NullPointerException("Full path and base path cannot be null for relativization.");
        }
        try {
            Path full = Paths.get(fullPath);
            Path base = Paths.get(basePath);
            return base.relativize(full).toString();
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Could not relativize paths: base='" + basePath + "', full='" + fullPath + "'. Returning full path. Error: " + e.getMessage());
            return fullPath;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error relativizing paths: base='" + basePath + "', full='" + fullPath + "'. Returning full path.", e);
            return fullPath;
        }
    }

    /**
     * Reads and concatenates the contents of included files into a single formatted string.
     * Each file's content is prefixed with its path. Skips files that cannot be read.
     *
     * @param includedFiles A list of paths to included files.
     * @return A formatted string containing the concatenated contents, or an empty string if the list is null/empty or no files could be read.
     */
    public static String getIncludedFilesContent(List<String> includedFiles) {
        if (includedFiles == null || includedFiles.isEmpty()) {
            return "";
        }

        StringBuilder outStr = new StringBuilder();
        for (String filePath : includedFiles) {
            String content = readFile(filePath);
            if (content == null || content.startsWith("Error reading ")) {
                continue;
            }
            outStr.append("file_path: `").append(filePath).append("`\n")
                    .append("content:\n```\n").append(content).append("\n```\n");
        }

        return outStr.toString().trim();
    }
}