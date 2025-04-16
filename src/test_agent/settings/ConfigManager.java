package test_agent.settings;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Singleton class for managing application configuration using Typesafe Config.
 * Loads configuration from multiple HOCON files and provides access to the merged configuration.
 */
public class ConfigManager {
    private static final Logger LOGGER = Logger.getLogger(ConfigManager.class.getName());
    private static ConfigManager instance;
    private final Config config;

    // List of configuration files to load
    private static final List<String> CONFIG_FILES = Arrays.asList(
            "test_generation_prompt.conf",
            "language_extensions.conf",
            "analyze_test_run_failure.conf",
            "analyze_test_against_context.conf",
            "analyze_suite_test_insert_line.conf",
            "adapt_test_command_for_a_single_test_via_ai.conf"
    );

    /**
     * Private constructor to enforce singleton pattern.
     * Loads and merges configuration from all specified files.
     *
     * @throws RuntimeException if any required configuration file is missing
     */
    private ConfigManager() {
        // Create a base config to merge others into
        Config mergedConfig = ConfigFactory.empty();
        boolean foundAnyConfig = false;

        for (String configFile : CONFIG_FILES) {
            boolean foundConfig = false;

            // First try to load from classpath resources (inside JAR)
            try {
                // Try different possible paths within the classpath
                String[] classpathLocations = {
                        configFile,                           // Root of classpath
                        "test_agent/settings/" + configFile,  // In settings package
                        configFile.replace(".conf", ""),      // Without extension
                        "test_agent/settings/" + configFile.replace(".conf", "") // In settings package without extension
                };

                for (String resourcePath : classpathLocations) {
                    InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                    if (resourceStream != null) {
                      //  LOGGER.info("Found config in classpath: " + resourcePath);
                        try {
                            Config fileConfig = ConfigFactory.parseReader(
                                    new InputStreamReader(resourceStream),
                                    ConfigParseOptions.defaults()
                            );
                            mergedConfig = fileConfig.withFallback(mergedConfig);
                            foundConfig = true;
                            foundAnyConfig = true;
                            break;
                        } finally {
                            resourceStream.close();
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading config from classpath: " + e.getMessage(), e);
            }

            // If not found in classpath, try file system locations
            if (!foundConfig) {
                // Try multiple possible locations for config files
                List<String> possibleLocations = Arrays.asList(
                        // Target classes directory
                        "target/classes",
                        // Source directory
                        "src/main/java/test_agent/settings",
                        // Source resources directory
                        "src/main/resources",
                        // Current directory
                        ".",
                        // Settings directory in resources
                        "src/main/resources/test_agent/settings",
                        // Plugin directory (for Eclipse plugins)
                        System.getProperty("user.dir"),
                        // Eclipse workspace directory
                        System.getProperty("user.dir") + "/configuration"
                );

                // Try each possible location
                for (String location : possibleLocations) {
                    Path filePath = Paths.get(location, configFile);
                   // LOGGER.info("Checking for config at: " + filePath.toAbsolutePath());

                    if (Files.exists(filePath)) {
                      //  LOGGER.info("Found config at: " + filePath.toAbsolutePath());
                        // Load the config file and merge it with our accumulated config
                        Config fileConfig = ConfigFactory.parseFile(
                                filePath.toFile(),
                                ConfigParseOptions.defaults()
                        );

                        mergedConfig = fileConfig.withFallback(mergedConfig);
                        foundConfig = true;
                        foundAnyConfig = true;
                        break;
                    }
                }
            }

            if (!foundConfig) {
                LOGGER.warning("Could not find configuration file: " + configFile);
            }
        }

        if (!foundAnyConfig) {
            throw new RuntimeException("No configuration files found in any of the expected locations");
        }

        // Resolve the final configuration with substitutions
        this.config = mergedConfig.resolve(ConfigResolveOptions.defaults());
    }

    /**
     * Gets the singleton instance of ConfigManager.
     *
     * @return the ConfigManager instance
     */
    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    /**
     * Gets the loaded configuration.
     *
     * @return the Config object containing all merged configuration
     */
    public Config getConfig() {
        return config;
    }

    /**
     * For debugging purposes - prints all attempted classpath locations
     */
    public static void debugClasspath() {
       // LOGGER.info("Classpath: " + System.getProperty("java.class.path"));

        for (String configFile : CONFIG_FILES) {
            String[] paths = {
                    configFile,
                    "test_agent/settings/" + configFile,
                    configFile.replace(".conf", ""),
                    "test_agent/settings/" + configFile.replace(".conf", "")
            };

            for (String path : paths) {
                InputStream stream = ConfigManager.class.getClassLoader().getResourceAsStream(path);
              //  LOGGER.info("Resource " + path + ": " + (stream != null ? "FOUND" : "NOT FOUND"));
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }
    }
}