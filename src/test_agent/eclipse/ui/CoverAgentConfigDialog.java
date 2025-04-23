package test_agent.eclipse.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.window.Window;
import org.eclipse.core.resources.IContainer;

import test_agent.eclipse.util.DependencyResolver;
import test_agent.eclipse.util.SecureStorageUtil;
import org.eclipse.core.runtime.jobs.Job;


import test_agent.eclipse.CoverAgent;
import test_agent.eclipse.CoverAgentArgs;
import test_agent.eclipse.job.RunCoverAgentJob;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Dialog for configuring CoverAgent parameters.
 * This dialog allows users to select and configure all necessary parameters
 * for running CoverAgent.
 */
public class CoverAgentConfigDialog extends TitleAreaDialog {

    private static final Logger logger = Logger.getLogger(CoverAgentConfigDialog.class.getName());
    
    private final IJavaProject javaProject;
    private final IFile selectedFile;
    
    private Text sourceFileText;
    private Text testFileText;
    private Text testOutputFileText;
    private Text coverageReportText;
    private Text testCommandText;
    private Combo modelCombo;
    private Text testCommandDirText;
    private CheckboxTableViewer includedFilesViewer;
    private Combo coverageTypeCombo;
    private Spinner desiredCoverageSpinner;
    private Text additionalInstructionsText;
    private Text projectRootText;
    private Spinner maxIterationsSpinner;
    private Button runEachTestSeparatelyCheckbox;
    private Spinner runTestsMultipleTimesSpinner;
    private Text apiKeyText;
    private Text siteUrlText;
    private Text siteNameText;    
    private List<String> includedFilePaths = new ArrayList<>();

    /**
     * Create the dialog.
     * 
     * @param parentShell The parent shell
     * @param javaProject The Java project
     * @param selectedFile The selected file (may be null)
     */
    public CoverAgentConfigDialog(Shell parentShell, IJavaProject javaProject, IFile selectedFile) {
        super(parentShell);
        this.javaProject = javaProject;
        this.selectedFile = selectedFile;
    }


    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        
        ScrolledComposite scrolledComposite = new ScrolledComposite(area, SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        Composite container = new Composite(scrolledComposite, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        
        setTitle("CoverAgent Configuration");
        setMessage("Configure parameters for running CoverAgent");

        createFilePathsGroup(container);
        createCommandGroup(container);
        createCoverageGroup(container);
        createAdvancedOptionsGroup(container);
        createApiConfigGroup(container);
        initializeValues();
        
        scrolledComposite.setContent(container);
        scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        
        return area;
    }


    /**
     * Create the file paths group
     * 
     * @param container The parent container
     */
    private void createFilePathsGroup(Composite container) {
        Group fileGroup = new Group(container, SWT.NONE);
        fileGroup.setText("File Paths");
        fileGroup.setLayout(new GridLayout(3, false));
        fileGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
        
        Label sourceFileLabel = new Label(fileGroup, SWT.NONE);
        sourceFileLabel.setText("Source File:");
        
        sourceFileText = new Text(fileGroup, SWT.BORDER);
        sourceFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Button sourceFileBrowseButton = new Button(fileGroup, SWT.PUSH);
        sourceFileBrowseButton.setText("Browse...");
        sourceFileBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browseForFile(sourceFileText, "Select Source File", "*.java");
            }
        });
        
        Label testFileLabel = new Label(fileGroup, SWT.NONE);
        testFileLabel.setText("Test File:");
        
        testFileText = new Text(fileGroup, SWT.BORDER);
        testFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Button testFileBrowseButton = new Button(fileGroup, SWT.PUSH);
        testFileBrowseButton.setText("Browse...");
        testFileBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browseForFile(testFileText, "Select Test File", "*.java");
            }
        });
        
      
        Label testOutputFileLabel = new Label(fileGroup, SWT.NONE);
        testOutputFileLabel.setText("Test Output File:");
        
        testOutputFileText = new Text(fileGroup, SWT.BORDER);
        testOutputFileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Button testOutputFileBrowseButton = new Button(fileGroup, SWT.PUSH);
        testOutputFileBrowseButton.setText("Browse...");
        testOutputFileBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browseForFile(testOutputFileText, "Select Test Output File", "*.java");
            }
        });
        
        Label coverageReportLabel = new Label(fileGroup, SWT.NONE);
        coverageReportLabel.setText("Coverage Report:");
        
        coverageReportText = new Text(fileGroup, SWT.BORDER);
        coverageReportText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Button coverageReportBrowseButton = new Button(fileGroup, SWT.PUSH);
        coverageReportBrowseButton.setText("Browse...");
        coverageReportBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browseForFile(coverageReportText, "Select Coverage Report", "*.xml");
            }
        });
        
        Label projectRootLabel = new Label(fileGroup, SWT.NONE);
        projectRootLabel.setText("Project Root:");
        
        projectRootText = new Text(fileGroup, SWT.BORDER);
        projectRootText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Button projectRootBrowseButton = new Button(fileGroup, SWT.PUSH);
        projectRootBrowseButton.setText("Browse...");
        projectRootBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browseForDirectory(projectRootText, "Select Project Root");
            }
        });
        
        Label includedFilesLabel = new Label(fileGroup, SWT.NONE);
        includedFilesLabel.setText("Included Files:");
        includedFilesLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));
        
        includedFilesViewer = CheckboxTableViewer.newCheckList(fileGroup, SWT.BORDER | SWT.V_SCROLL);
        includedFilesViewer.setContentProvider(ArrayContentProvider.getInstance());
        includedFilesViewer.setLabelProvider(new LabelProvider());
        GridData viewerData = new GridData(SWT.FILL, SWT.FILL, true, false);
        viewerData.heightHint = 100;
        includedFilesViewer.getTable().setLayoutData(viewerData);
        
        Composite buttonComposite = new Composite(fileGroup, SWT.NONE);
        buttonComposite.setLayout(new GridLayout(1, false));
        buttonComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));
        
        Button addButton = new Button(buttonComposite, SWT.PUSH);
        addButton.setText("Add...");
        addButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addIncludedFile();
            }
        });
        
        Button removeButton = new Button(buttonComposite, SWT.PUSH);
        removeButton.setText("Remove");
        removeButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                removeIncludedFile();
            }
        });
    }

    /**
     * Create the command group
     * 
     * @param container The parent container
     */
    private void createCommandGroup(Composite container) {
        Group commandGroup = new Group(container, SWT.NONE);
        commandGroup.setText("Command Configuration");
        commandGroup.setLayout(new GridLayout(3, false));
        commandGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
        
        Label testCommandLabel = new Label(commandGroup, SWT.NONE);
        testCommandLabel.setText("Test Command:");
        
        testCommandText = new Text(commandGroup, SWT.BORDER);
        testCommandText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        Label testCommandDirLabel = new Label(commandGroup, SWT.NONE);
        testCommandDirLabel.setText("Test Command Directory:");
        
        testCommandDirText = new Text(commandGroup, SWT.BORDER);
        testCommandDirText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Button testCommandDirBrowseButton = new Button(commandGroup, SWT.PUSH);
        testCommandDirBrowseButton.setText("Browse...");
        testCommandDirBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browseForDirectory(testCommandDirText, "Select Test Command Directory");
            }
        });
        
        Label modelLabel = new Label(commandGroup, SWT.NONE);
        modelLabel.setText("Model:");
        
        modelCombo = new Combo(commandGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        modelCombo.setItems(new String[] {
        	"deepseek/deepseek-chat-v3-0324:free"
        	
            
            
        });
        modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    }

    /**
     * Create the coverage group
     * 
     * @param container The parent container
     */
    private void createCoverageGroup(Composite container) {
        Group coverageGroup = new Group(container, SWT.NONE);
        coverageGroup.setText("Coverage Configuration");
        coverageGroup.setLayout(new GridLayout(2, false));
        coverageGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
        
        Label coverageTypeLabel = new Label(coverageGroup, SWT.NONE);
        coverageTypeLabel.setText("Coverage Type:");
        
        coverageTypeCombo = new Combo(coverageGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        coverageTypeCombo.setItems(new String[] {"jacoco"});
        coverageTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        Label desiredCoverageLabel = new Label(coverageGroup, SWT.NONE);
        desiredCoverageLabel.setText("Desired Coverage (%):");
        
        desiredCoverageSpinner = new Spinner(coverageGroup, SWT.BORDER);
        desiredCoverageSpinner.setMinimum(0);
        desiredCoverageSpinner.setMaximum(100);
        desiredCoverageSpinner.setIncrement(1);
        desiredCoverageSpinner.setPageIncrement(10);
        desiredCoverageSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
 
    }

    /**
     * Create the advanced options group
     * 
     * @param container The parent container
     */
    private void createAdvancedOptionsGroup(Composite container) {
        Group advancedGroup = new Group(container, SWT.NONE);
        advancedGroup.setText("Advanced Options");
        advancedGroup.setLayout(new GridLayout(2, false));
        advancedGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
        
        Label maxIterationsLabel = new Label(advancedGroup, SWT.NONE);
        maxIterationsLabel.setText("Max Iterations:");
        
        maxIterationsSpinner = new Spinner(advancedGroup, SWT.BORDER);
        maxIterationsSpinner.setMinimum(1);
        maxIterationsSpinner.setMaximum(5);
        maxIterationsSpinner.setIncrement(1);
        maxIterationsSpinner.setPageIncrement(5);
        maxIterationsSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        runEachTestSeparatelyCheckbox = new Button(advancedGroup, SWT.CHECK);
        runEachTestSeparatelyCheckbox.setText("Run Each Test Separately");
        runEachTestSeparatelyCheckbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        
        Label runTestsMultipleTimesLabel = new Label(advancedGroup, SWT.NONE);
        runTestsMultipleTimesLabel.setText("Run Tests Multiple Times:");
        
        runTestsMultipleTimesSpinner = new Spinner(advancedGroup, SWT.BORDER);
        runTestsMultipleTimesSpinner.setMinimum(1);
        runTestsMultipleTimesSpinner.setMaximum(10);
        runTestsMultipleTimesSpinner.setIncrement(1);
        runTestsMultipleTimesSpinner.setPageIncrement(1);
        runTestsMultipleTimesSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        Label additionalInstructionsLabel = new Label(advancedGroup, SWT.NONE);
        additionalInstructionsLabel.setText("Additional Instructions:");
        additionalInstructionsLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));
        
        additionalInstructionsText = new Text(advancedGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData textData = new GridData(SWT.FILL, SWT.FILL, true, true);
        textData.heightHint = 60;
        additionalInstructionsText.setLayoutData(textData);
    }

    /**
     * Create the API configuration group
     * 
     * @param container The parent container
     */
    private void createApiConfigGroup(Composite container) {
        Group apiGroup = new Group(container, SWT.NONE);
        apiGroup.setText("API Configuration");
        apiGroup.setLayout(new GridLayout(2, false));
        
        GridData apiGroupData = new GridData(SWT.FILL, SWT.FILL, true, false);
        apiGroupData.horizontalSpan = 1;  
        apiGroup.setLayoutData(apiGroupData);

        Label apiKeyLabel = new Label(apiGroup, SWT.NONE);
        apiKeyLabel.setText("OpenRouter API Key (Required):");
        
        apiKeyText = new Text(apiGroup, SWT.BORDER | SWT.PASSWORD);
        apiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label apiKeyNoteLabel = new Label(apiGroup, SWT.NONE);
        apiKeyNoteLabel.setText("Get an API key from openrouter.ai");
        apiKeyNoteLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Label siteUrlLabel = new Label(apiGroup, SWT.NONE);
        siteUrlLabel.setText("Site URL:");
        
        siteUrlText = new Text(apiGroup, SWT.BORDER);
        siteUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label siteNameLabel = new Label(apiGroup, SWT.NONE);
        siteNameLabel.setText("Site Name:");
        
        siteNameText = new Text(apiGroup, SWT.BORDER);
        siteNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    /**
     * Initialize the dialog with default values
     */
    private void initializeValues() {
        if (javaProject != null) {
            IProject project = javaProject.getProject();
            projectRootText.setText(project.getLocation().toOSString());
            testCommandDirText.setText(project.getLocation().toOSString());
            
            if (isMavenProject(project)) {
                testCommandText.setText("mvn -f \"" + project.getLocation().toOSString() + "\" clean test");
            } else if (isGradleProject(project)) {
                testCommandText.setText("./gradlew test");
            }
            
            if (isMavenProject(project)) {
                coverageReportText.setText(project.getLocation().append("target/site/jacoco/jacoco.xml").toOSString());
            } else if (isGradleProject(project)) {
                coverageReportText.setText(project.getLocation().append("build/reports/jacoco/test/jacocoTestReport.xml").toOSString());
            }
            
            siteUrlText.setText("http://localhost");
            siteNameText.setText("EclipseCoverAgentPlugin");
            
            if (selectedFile != null) {
                  try {
                      String path = selectedFile.getLocation().toOSString();
                      if (sourceFileText == null) {
                           logger.severe("initializeValues: sourceFileText is NULL before setting text!");
                      } else {
                           sourceFileText.setText(path);
                           autoPopulateIncludedFiles();
                      
                      }
                  } catch (Exception ex) {
                      logger.log(java.util.logging.Level.SEVERE, "initializeValues: Error setting sourceFileText", ex);
                 }
                sourceFileText.setText(selectedFile.getLocation().toOSString());
                
                String fileName = selectedFile.getName();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                
                try {
                    IFolder testFolder = project.getFolder("src/test/java"); 
                    if (testFolder.exists()) {
                        IFile foundTestFile = findTestFileRecursive(testFolder, baseName + "Test.java"); 
                        if (foundTestFile != null && foundTestFile.exists()) { 
                             IPath location = foundTestFile.getLocation();
                             if (location != null) {
                                 testFileText.setText(location.toOSString());
                                 logger.info("Automatically found and set test file: " + location.toOSString());
                             } else {
                                 logger.warning("Found test file resource, but could not get its location: " + foundTestFile.getFullPath());
                             }
                        } else {
                             logger.info("Could not automatically find test file named " + baseName + "Test.java" + " in " + testFolder.getFullPath() + " or its subfolders.");
                        }
                    } else {
                         logger.warning("Default test folder src/test/java does not exist in project " + project.getName());
                    }
                } catch (CoreException e) {
                    logger.warning("Error finding test folder or searching within it: " + e.getMessage()); 
                }
            }
            
            String savedApiKey = SecureStorageUtil.getApiKey();
            if (savedApiKey != null && !savedApiKey.isEmpty()) {
                apiKeyText.setText(savedApiKey);
               // logger.info("Loaded API key from secure storage");
            }
        }
        
        // Set default values for other fields
        modelCombo.select(0);
        coverageTypeCombo.select(0);
        desiredCoverageSpinner.setSelection(100);
        maxIterationsSpinner.setSelection(2);
        runTestsMultipleTimesSpinner.setSelection(1);
        siteUrlText.setText("http://localhost");
        siteNameText.setText("EclipseCoverAgentPlugin");
        

    }
    /**
     * Recursively searches for a file with the given name within a container (folder/project).
     *
     * @param container The container to search within.
     * @param targetFileName The exact name of the file to find.
     * @return The IFile if found, otherwise null.
     * @throws CoreException If an error occurs accessing resources.
     */
    private IFile findTestFileRecursive(IContainer container, String targetFileName) throws CoreException {
        if (container == null || !container.exists()) {
            return null; // Nothing to search if container is invalid
        }
        for (IResource resource : container.members()) {
            if (resource.getType() == IResource.FILE && resource.getName().equals(targetFileName)) {
                return (IFile) resource;
            } else if (resource.getType() == IResource.FOLDER) {
                IFile foundInSubfolder = findTestFileRecursive((IFolder) resource, targetFileName);
                if (foundInSubfolder != null) {
                    return foundInSubfolder;
                }
            }
        }
        return null;
    } 

    /**
     * Check if the project is a Maven project
     * 
     * @param project The project to check
     * @return true if it's a Maven project, false otherwise
     */
    private boolean isMavenProject(IProject project) {
        return project.getFile("pom.xml").exists();
    }

    /**
     * Check if the project is a Gradle project
     *
     * @param project The project to check
     * @return true if it's a Gradle project, false otherwise
     */
    private boolean isGradleProject(IProject project) {
        return project.getFile("build.gradle").exists();
    }

    /**
     * Find a test file in the given folder
     *
     * @param folder The folder to search
     * @param testFileName The name of the test file to find
     * @throws CoreException If an error occurs while accessing the folder
     */
    private void findTestFile(IFolder folder, String testFileName) throws CoreException {
        for (IResource resource : folder.members()) {
            if (resource instanceof IFile && resource.getName().equals(testFileName)) {
                testFileText.setText(resource.getLocation().toOSString());
                return;
            }
        }
    }

   
    
    
    
    /**
     * Browse for a file in the Eclipse workspace and set the selected path to the given text field
     *
     * @param text The text field to set the path
     * @param dialogTitle The title of the file dialog
     * @param filterExtensions The file extensions to filter
     */
    private void browseForFile(Text text, String dialogTitle, String... filterExtensions) {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
                getShell(),
                new WorkbenchLabelProvider(),
                new WorkbenchContentProvider());
        
        dialog.setTitle(dialogTitle);
        dialog.setMessage("Select a file:");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IFile) {
                    IFile file = (IFile) element;
                    for (String extension : filterExtensions) {
                        // Convert filter pattern like "*.java" to just "java"
                        String ext = extension.replace("*.", "");
                        if (ext.equals(file.getFileExtension())) {
                            return true;
                        }
                    }
                    return false;
                }
                return element instanceof IFolder || element instanceof IProject;
            }
        });
        
        if (dialog.open() == Window.OK) {
            Object result = dialog.getFirstResult();
            if (result instanceof IFile) {
                IFile file = (IFile) result;
                text.setText(file.getLocation().toOSString());
            }
        }
    }

    /**
     * Browse for a directory in the Eclipse workspace and set the selected path to the given text field
     *
     * @param text The text field to set the path
     * @param dialogTitle The title of the directory dialog
     */
    private void browseForDirectory(Text text, String dialogTitle) {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
                getShell(),
                new WorkbenchLabelProvider(),
                new WorkbenchContentProvider());
        
        dialog.setTitle(dialogTitle);
        dialog.setMessage("Select a folder:");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
        dialog.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return element instanceof IFolder || element instanceof IProject;
            }
        });
        
        if (dialog.open() == Window.OK) {
            Object result = dialog.getFirstResult();
            if (result instanceof IContainer) {
                IContainer container = (IContainer) result;
                text.setText(container.getLocation().toOSString());
            }
        }
    }
    /**
     * Attempts to automatically populate the 'Included Files' list based on the
     * import statements in the currently selected source file.
     * Requires sourceFileText to be populated and point to a valid file.
     */
    private void autoPopulateIncludedFiles() {
        includedFilePaths.clear(); 

        String sourcePathString = sourceFileText.getText();
        if (sourcePathString == null || sourcePathString.trim().isEmpty()) {
            logger.warning("Source file path is empty, cannot auto-populate dependencies.");
            updateIncludedFilesViewer(); 
            return;
        }

        IPath sourcePath = Path.fromOSString(sourcePathString);
        IFile sourceIFile = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(sourcePath);

        if (sourceIFile == null || !sourceIFile.exists()) {
            logger.warning("Could not find IFile in workspace for source path: " + sourcePathString + ". Cannot auto-populate dependencies.");
            updateIncludedFilesViewer();
            return;
        }

        if (!"java".equalsIgnoreCase(sourceIFile.getFileExtension())) {
             logger.warning("Source file is not a .java file: " + sourcePathString + ". Skipping dependency resolution.");
             updateIncludedFilesViewer();
             return;
        }

        ICompilationUnit sourceCU = JavaCore.createCompilationUnitFrom(sourceIFile);

        if (sourceCU == null || !sourceCU.exists()) {
             logger.warning("Could not create ICompilationUnit for: " + sourcePathString + ". Cannot auto-populate dependencies.");
             updateIncludedFilesViewer();
             return;
        }

        List<IFile> dependencies = DependencyResolver.collect(sourceCU);

        List<String> dependencyPaths = dependencies.stream()
            .map(file -> {
                IPath location = file.getLocation();
                return (location != null) ? location.toOSString() : null;
            })
            .filter(path -> path != null && !path.trim().isEmpty())
            .distinct() 
            .collect(Collectors.toList());

        includedFilePaths.addAll(dependencyPaths);

        updateIncludedFilesViewer();

        if (!includedFilePaths.isEmpty()) {
            includedFilesViewer.setAllChecked(true);
            logger.info("Auto-populated and checked " + includedFilePaths.size() + " included files based on imports.");
        } else {
             logger.info("No project-local source file dependencies found via imports.");
        }
    }
  
    
    
    /**
     * Helper method to update the included files viewer based on the current
     * state of the includedFilePaths list.
     */
    private void updateIncludedFilesViewer() {
         if (includedFilesViewer != null && !includedFilesViewer.getTable().isDisposed()) {
              // Set input to refresh the viewer with the current list contents
              includedFilesViewer.setInput(includedFilePaths);
              // Optional: uncomment refresh if setInput isn't enough in some edge cases
              // includedFilesViewer.refresh();
         } else {
              logger.warning("IncludedFilesViewer is null or disposed. Cannot update UI.");
         }
    }
    

    /**
     * Add a file to the included files list
     */
    private void addIncludedFile() {
        ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
                getShell(),
                new WorkbenchLabelProvider(), 
                new WorkbenchContentProvider()); 

        dialog.setTitle("Select Included Files");
        dialog.setMessage("Select one or more files from the workspace to include:");
        dialog.setInput(ResourcesPlugin.getWorkspace().getRoot()); 
        dialog.setAllowMultiple(true); 

        
        dialog.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof IContainer) { 
                    return true; 
                }
                if (element instanceof IFile) {
                    // Optional: Filter specific file types here if needed, e.g., Java files
                    // return "java".equals(((IFile) element).getFileExtension());
                    return true; // Show all files, validator will handle selection validity
                }
                return false;
            }
        });

        // Validator: Ensure only IFile resources can be selected for the final result.
        dialog.setValidator(new ISelectionStatusValidator() {
            @Override
            public IStatus validate(Object[] selection) {
                if (selection == null || selection.length == 0) {
                    // You might want to return OK_STATUS if allowing OK with no selection,
                    // but usually, you want the user to select at least one.
                    // return Status.OK_STATUS;
                    return new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, "Please select at least one file.");
                }
                for (Object obj : selection) {
                    if (!(obj instanceof IFile)) {
                        // If anything other than a file is selected (e.g., folder, project), it's an error.
                        return new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, "Selection must contain only files.");
                    }
                    // Optional: Add check for file extension here if needed
                    // IFile file = (IFile) obj;
                    // if (!"java".equals(file.getFileExtension())) {
                    //    return new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, "Only Java files can be selected.");
                    // }
                }
                // All selected items are files
                return Status.OK_STATUS;
            }
        });

        // Open the dialog
        if (dialog.open() == Window.OK) {
            Object[] results = dialog.getResult(); // **** GET MULTIPLE RESULTS ****
            boolean changed = false; // Track if we actually added anything new

            if (results != null) {
                for (Object result : results) {
                    if (result instanceof IFile) {
                        IFile file = (IFile) result;
                        IPath location = file.getLocation(); // Get the absolute file system path
                        if (location != null) {
                            String path = location.toOSString();
                            // Add to list if not already present to avoid duplicates
                            if (!includedFilePaths.contains(path)) {
                                includedFilePaths.add(path);
                                changed = true;
                            }
                        } else {
                             logger.warning("Could not get location for selected file: " + file.getFullPath());
                             // Handle linked resources or other cases where location might be null if necessary
                        }
                    }
                }
            }

            // Update the viewer *only if* changes were made
            if (changed) {
                // Resetting the input is often the easiest way to update the viewer
                // after modifying the underlying list.
                includedFilesViewer.setInput(includedFilePaths);
                // includedFilesViewer.refresh(); // May also work depending on provider and list type
            }
        }
    }

    /**
     * Remove a selected file from the included files list
     */
    private void removeIncludedFile() {
        Object[] checkedElements = includedFilesViewer.getCheckedElements();

        if (checkedElements != null && checkedElements.length > 0) {
  
            List<?> itemsToRemove = Arrays.asList(checkedElements);

            includedFilePaths.removeAll(itemsToRemove);

     
            includedFilesViewer.setInput(includedFilePaths);

   
        }
    }

 
        @Override
        protected void okPressed() {
            //  Validation 
            setErrorMessage(null); 

            String sourceFilePath = sourceFileText.getText().trim();
            if (sourceFilePath.isEmpty()) {
                setErrorMessage("Source File path cannot be empty.");
                sourceFileText.setFocus(); 
                return; 
            }
            if (!Files.exists(Paths.get(sourceFilePath))) {
                setErrorMessage("Source File does not exist: " + sourceFilePath);
                sourceFileText.setFocus();
                return; 
            }

            String testFilePath = testFileText.getText().trim();
            if (testFilePath.isEmpty()) {
                 setErrorMessage("Test File path cannot be empty.");
                 testFileText.setFocus();
                 return;
            }



            String testCommand = testCommandText.getText().trim();
            if (testCommand.isEmpty()) {
                setErrorMessage("Test Command cannot be empty.");
                testCommandText.setFocus();
                return;
            }

            String projectRootPath = projectRootText.getText().trim();
            if (projectRootPath.isEmpty()) {
                setErrorMessage("Project Root path cannot be empty.");
                projectRootText.setFocus();
                return;
            }
            if (!Files.isDirectory(Paths.get(projectRootPath))) {
                 setErrorMessage("Project Root path must be a valid directory: " + projectRootPath);
                 projectRootText.setFocus();
                 return;
            }

            String apiKey = apiKeyText.getText().trim();
            if (apiKey.isEmpty()) {
                setErrorMessage("OpenRouter API key is required. Please enter a valid API key from openrouter.ai");
                apiKeyText.setFocus(); 
                return; 
            }
            SecureStorageUtil.saveApiKey(apiKey);
  
        

        CoverAgentArgs args = new CoverAgentArgs.Builder()
                .sourceFilePath(sourceFileText.getText())
                .testFilePath(testFileText.getText())
                .testFileOutputPath(testOutputFileText.getText())
                .codeCoverageReportPath(coverageReportText.getText())
                .testCommand(testCommandText.getText())
                .model(modelCombo.getText())
                .testCommandDir(testCommandDirText.getText())
                .includedFiles(includedFilePaths)
                .coverageType(coverageTypeCombo.getText())
                .desiredCoverage(desiredCoverageSpinner.getSelection())
                .additionalInstructions(additionalInstructionsText.getText())
                .projectRoot(projectRootText.getText())
                .maxIterations(maxIterationsSpinner.getSelection())
                .runEachTestSeparately(runEachTestSeparatelyCheckbox.getSelection())
                .runTestsMultipleTimes(runTestsMultipleTimesSpinner.getSelection())
                .apiKey(apiKeyText.getText())
                .siteUrl(siteUrlText.getText())
                .siteName(siteNameText.getText())
                .build();
          test_agent.eclipse.util.CoverAgentConsole.install();

        Job job = new RunCoverAgentJob("Running CoverAgent", args);
        job.schedule(); 

        super.okPressed();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Run", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }
}