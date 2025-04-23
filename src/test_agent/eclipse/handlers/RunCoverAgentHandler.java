package test_agent.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource; 
import org.eclipse.core.runtime.CoreException; 
import org.eclipse.jdt.core.ICompilationUnit; 
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import test_agent.eclipse.ui.CoverAgentConfigDialog;

import java.util.logging.Level; 
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
            if (structuredSelection.isEmpty()) {
                //logger.info("Handler: Selection is empty.");
                return null; // Nothing selected
            }
            Object firstElement = structuredSelection.getFirstElement();

            IJavaProject javaProject = null;
            IFile selectedFile = null;

            if (firstElement instanceof IJavaElement) {
                IJavaElement javaElement = (IJavaElement) firstElement;
                javaProject = javaElement.getJavaProject(); 

                
                if (javaElement instanceof ICompilationUnit) {
                    //logger.info("Handler: Selected element is an ICompilationUnit.");
                    try {
                        
                        IResource resource = javaElement.getCorrespondingResource();
                        if (resource instanceof IFile && "java".equals(resource.getFileExtension())) {
                            selectedFile = (IFile) resource;
                            //logger.info("Handler: Corresponding resource is an IFile: " + selectedFile.getName());
                         
                            handleJavaFile(selectedFile, window);
                            return null; // Handled, exit
                        } else {
                            logger.warning("Handler: ICompilationUnit did not correspond to a java IFile.");
                        }
                    } catch (CoreException e) {
                        logger.log(Level.SEVERE, "Handler: Error getting corresponding resource for ICompilationUnit", e);
                        
                    }
                }
                
                else if (javaElement instanceof IJavaProject) {
                    // logger.info("Handler: Selected element is an IJavaProject.");
                    
                }
                 else {
                    //logger.info("Handler: Selected element is another type of IJavaElement (e.g., package): " + javaElement.getClass().getName() + ". Treating as project selection.");
                    
                }

                
                if (selectedFile == null && javaProject != null) {
                     handleJavaProject(javaProject, window);
                     return null;
                }

            }
            
            else if (firstElement instanceof IFile) {
                IFile file = (IFile) firstElement;
                if ("java".equals(file.getFileExtension())) {
                  //  logger.info("Handler: Selected element is directly an IFile.");
                    handleJavaFile(file, window);
                    return null; 
                }
            }
             
             else if (firstElement instanceof IProject) {
                 IProject project = (IProject) firstElement;
                 javaProject = JavaCore.create(project);
                 if (javaProject != null && javaProject.exists()) {
                    // logger.info("Handler: Selected element is directly an IProject.");
                     handleJavaProject(javaProject, window);
                     return null; // Handled, exit
                 }
             }

            if (javaProject == null && selectedFile == null) {
                 logger.warning("Handler: Could not determine appropriate action for selected element: " + firstElement.getClass().getName());
            }

        } else {
             //logger.info("Handler: Selection is not an IStructuredSelection.");
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
            logger.info("Handler: handleJavaFile called for: " + file.getFullPath().toString());
        } else {
            logger.warning("Handler: handleJavaFile called but file is NULL!");
            return; 
        }

        CoverAgentConfigDialog dialog = new CoverAgentConfigDialog(window.getShell(), javaProject, file);
        dialog.open();
    }

    /**
     * Handle a selected Java project (or when a specific file wasn't identified)
     *
     * @param javaProject The selected Java project
     * @param window The active workbench window
     */
    private void handleJavaProject(IJavaProject javaProject, IWorkbenchWindow window) {
        if (javaProject != null) {
            logger.info("Handler: handleJavaProject called for: " + javaProject.getElementName());
        } else {
             logger.warning("Handler: handleJavaProject called but javaProject is NULL!");
             return; 
        }

        CoverAgentConfigDialog dialog = new CoverAgentConfigDialog(window.getShell(), javaProject, null);
        dialog.open();
    }
}