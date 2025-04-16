package test_agent.utils;

import com.typesafe.config.Config;
import test_agent.settings.ConfigManager; // Assuming ConfigManager is accessible
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class LanguageUtils {
    private static final Logger LOGGER = Logger.getLogger(LanguageUtils.class.getName());
    private static final String CONFIG_KEY_LANG_EXT_MAP = "language_extension_map_org";
    private static final String UNKNOWN_LANGUAGE = "unknown";

    private LanguageUtils() {}

    private static Map<String, String> extensionToLanguageMap = null;

    /**
     * Gets the programming language based on the file extension using configuration.
     * Caches the mapping after the first call.
     *
     * @param sourceFilePath The path to the source file
     * @return The programming language inferred from the file extension (lowercase), or "unknown".
     */
    public static String getCodeLanguageFromPath(String sourceFilePath) {
        if (extensionToLanguageMap == null) {
            initializeLanguageMap();
        }

        if (sourceFilePath == null || sourceFilePath.isEmpty() || !sourceFilePath.contains(".")) {
            return UNKNOWN_LANGUAGE;
        }

        String extension = "." + sourceFilePath.substring(sourceFilePath.lastIndexOf(".") + 1);
        return extensionToLanguageMap.getOrDefault(extension, UNKNOWN_LANGUAGE);
    }

    private static synchronized void initializeLanguageMap() {
        if (extensionToLanguageMap != null) {
            return;
        }

        Map<String, String> tempMap = new HashMap<>();
        try {
            Config config = ConfigManager.getInstance().getConfig();
            if (config.hasPath(CONFIG_KEY_LANG_EXT_MAP)) {
                Config languageExtensionMapOrg = config.getConfig(CONFIG_KEY_LANG_EXT_MAP);

                for (String language : languageExtensionMapOrg.root().keySet()) {
                    List<String> extensions = languageExtensionMapOrg.getStringList(language);
                    for (String ext : extensions) {
                        if (ext != null && !ext.isBlank()) {
                            tempMap.put(ext.trim(), language.trim().toLowerCase());
                        }
                    }
                }
            } else {
                LOGGER.warning("Configuration key '" + CONFIG_KEY_LANG_EXT_MAP + "' not found.");
            }
        } catch (Exception e) {
            LOGGER.severe("Error initializing language extension map from configuration: " + e.getMessage());
        }
        extensionToLanguageMap = tempMap;
    }
}