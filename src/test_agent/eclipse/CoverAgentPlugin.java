package test_agent.eclipse;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import java.util.logging.Logger;

/**
 * The main plugin class for the CoverAgent Eclipse integration.
 * This class controls the plugin lifecycle.
 */
public class CoverAgentPlugin extends AbstractUIPlugin {

    // The plugin ID
    public static final String PLUGIN_ID = "test_agent.eclipse";

    // The shared instance
    private static CoverAgentPlugin plugin;
    
    private static final Logger logger = Logger.getLogger(CoverAgentPlugin.class.getName());

    /**
     * The constructor
     */
    public CoverAgentPlugin() {
        // Default constructor
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        logger.info("CoverAgent Eclipse plugin started");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
        logger.info("CoverAgent Eclipse plugin stopped");
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static CoverAgentPlugin getDefault() {
        return plugin;
    }
}