package test_agent.eclipse.job;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import test_agent.eclipse.CoverAgent;
import test_agent.eclipse.CoverAgentArgs;
import test_agent.eclipse.CoverAgentPlugin;
import test_agent.eclipse.util.ConsoleUtil;// Your plugin activator class

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RunCoverAgentJob extends Job {
	

    private static final Logger logger = Logger.getLogger(RunCoverAgentJob.class.getName());

    private final CoverAgentArgs args;
    

    public RunCoverAgentJob(String name, CoverAgentArgs args) {
        super(name);
        this.args = args;
        setUser(true); // Makes the job visible in the Progress view
    }

   
        @Override
        protected IStatus run(IProgressMonitor monitor) {
            monitor.beginTask("Running CoverAgent Test Generation", IProgressMonitor.UNKNOWN);
            
            // Show the console at the start
            ConsoleUtil.showConsole();
            ConsoleUtil.writeInfo("Starting CoverAgent test generation...");

            try {
                CoverAgent coverAgent = new CoverAgent(args, null);
                monitor.subTask("Generating tests...");
                coverAgent.run();

                if (monitor.isCanceled()) {
                    ConsoleUtil.writeInfo(getName());
                    return Status.CANCEL_STATUS;
                }

                monitor.done();
                ConsoleUtil.writeInfo("CoverAgent job completed successfully.");
                return Status.OK_STATUS;

            } catch (Exception e) {
                ConsoleUtil.writeError("Error during test generation: " + e.getMessage());
                return new Status(IStatus.ERROR, CoverAgentPlugin.PLUGIN_ID, "Test generation failed", e);
            }
        }

    
}