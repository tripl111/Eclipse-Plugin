package test_agent.eclipse.util;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for securely storing and retrieving sensitive information
 * such as API keys using Eclipse's secure storage mechanism.
 */
public class SecureStorageUtil {
    
    private static final Logger logger = Logger.getLogger(SecureStorageUtil.class.getName());
    
    // Node path in the secure storage
    private static final String SECURE_STORAGE_NODE = "test_agent.eclipse";
    
    // Keys for stored values
    private static final String API_KEY = "openrouter_api_key";
    
    /**
     * Saves the API key to secure storage
     * 
     * @param apiKey The API key to save
     * @return true if successful, false otherwise
     */
    public static boolean saveApiKey(String apiKey) {
        try {
            ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = securePreferences.node(SECURE_STORAGE_NODE);
            
            node.put(API_KEY, apiKey, true); // true means encrypt
            securePreferences.flush();
            
            logger.info("API key saved successfully to secure storage");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving API key to secure storage", e);
            return false;
        }
    }
    
    /**
     * Retrieves the API key from secure storage
     * 
     * @return The stored API key, or null if not found or an error occurs
     */
    public static String getApiKey() {
        try {
            ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = securePreferences.node(SECURE_STORAGE_NODE);
            
            if (node.keys().length > 0 && node.get(API_KEY, null) != null) {
                String apiKey = node.get(API_KEY, "");
                logger.info("API key retrieved successfully from secure storage");
                return apiKey;
            }
            
            return null;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Error retrieving API key from secure storage", e);
            return null;
        }
    }
    
    /**
     * Clears the API key from secure storage
     * 
     * @return true if successful, false otherwise
     * @throws IOException 
     */
    public static boolean clearApiKey() throws IOException {
        try {
            ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = securePreferences.node(SECURE_STORAGE_NODE);
            
            if (node.keys().length > 0 && node.get(API_KEY, null) != null) {
                node.remove(API_KEY);
                securePreferences.flush();
                logger.info("API key cleared successfully from secure storage");
            }
            
            return true;
        } catch (StorageException e) {
            logger.log(Level.SEVERE, "Error clearing API key from secure storage", e);
            return false;
        }
    }
}