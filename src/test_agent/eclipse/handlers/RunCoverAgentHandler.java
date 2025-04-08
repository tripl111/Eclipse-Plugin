package test_agent.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import test_agent.eclipse.ui.CoverAgentConfigDialog;

import java.util.logging.Logger;

/**
 * Handler for the "Run CoverAgent" command.
 * This class is responsible for handling the command execution when the user selects
 * the "Run CoverAgent" menu option.
 */
public class RunCoverAgentHandler extends AbstractHandler {

    private static final Logger logger = Logger.getLogger(RunCoverAgentHandler.class.getName());

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
        ISelectionService selectionService = window.getSelectionService();
        ISelection selection = selectionService.getSelection();

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection) selection;
            Object firstElement = structuredSelection.getFirstElement();

            // Handle Java file selection
            if (firstElement instanceof IFile) {
                IFile file = (IFile) firstElement;
                if ("java".equals(file.getFileExtension())) {
                    handleJavaFile(file, window);
                }
            } 
            // Handle Java element selection
            else if (firstElement instanceof IJavaElement) {
                IJavaElement javaElement = (IJavaElement) firstElement;
                IProject project = javaElement.getJavaProject().getProject();
                handleJavaProject(JavaCore.create(project), window);
            }
        }

        return null;
    }

    /**
     * Handle a selected Java file
     * 
     * @param file The selected Java file
     * @param window The active workbench window
     */
    private void handleJavaFile(IFile file, IWorkbenchWindow window) {
        IProject project = file.getProject();
        IJavaProject javaProject = JavaCore.create(project);
        
        if (file != null) {
            logger.info("Handler: handleJavaFile called for: " + file.getLocation().toOSString());
        } else {
            logger.warning("Handler: handleJavaFile called but file is NULL!");
        }
        
        // Open the configuration dialog
        CoverAgentConfigDialog dialog = new CoverAgentConfigDialog(window.getShell(), javaProject, file);
        dialog.open();
    }

    /**
     * Handle a selected Java project
     * 
     * @param javaProject The selected Java project
     * @param window The active workbench window
     */
    private void handleJavaProject(IJavaProject javaProject, IWorkbenchWindow window) {
        logger.info("Selected Java project: " + javaProject.getElementName());
        
        // Open the configuration dialog
        CoverAgentConfigDialog dialog = new CoverAgentConfigDialog(window.getShell(), javaProject, null);
        dialog.open();
    }
}