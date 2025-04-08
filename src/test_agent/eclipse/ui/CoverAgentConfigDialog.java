package test_agent.eclipse.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
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
import test_agent.eclipse.util.SecureStorageUtil;
import org.eclipse.core.runtime.jobs.Job;

import test_agent.CoverAgent;
import test_agent.CoverAgentArgs;
import test_agent.eclipse.job.RunCoverAgentJob;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Dialog for configuring CoverAgent parameters.
 * This dialog allows users to select and configure all necessary parameters
 * for running CoverAgent.
 */
public class CoverAgentConfigDialog extends TitleAreaDialog {

    private static final Logger logger = Logger.getLogger(CoverAgentConfigDialog.class.getName());
    
    private final IJavaProject javaProject;
    private final IFile selectedFile;
    
    // UI components
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
    private Button useReportCoverageCheckbox;
    private Text projectRootText;
    private Spinner maxIterationsSpinner;
    private Button diffCoverageCheckbox;
    private Button strictCoverageCheckbox;
    private Button runEachTestSeparatelyCheckbox;
    private Spinner runTestsMultipleTimesSpinner;
    private Text apiKeyText;
    private Text siteUrlText;
    private Text siteNameText;
    
    // Selected files
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
        
        // Create a ScrolledComposite to allow scrolling when dialog content is larger than visible area
        ScrolledComposite scrolledComposite = new ScrolledComposite(area, SWT.V_SCROLL);
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        // Create the main container for all components
        Composite container = new Composite(scrolledComposite, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        
        setTitle("CoverAgent Configuration");
        setMessage("Configure parameters for running CoverAgent");

        // File paths group
        createFilePathsGroup(container);

        // Command group
        createCommandGroup(container);

        // Coverage group
        createCoverageGroup(container);

        // Advanced options group
        createAdvancedOptionsGroup(container);

        // API configuration group
        createApiConfigGroup(container);

        // Initialize with default values
        initializeValues();
        
        // Set the scrolled content and compute the size
        scrolledComposite.setContent(container);
        scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        
        return area;
    }

    // Override getInitialSize to make the dialog larger
    @Override
    protected Point getInitialSize() {
        return new Point(1800, 2200); // Width, height - adjust as needed
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
        
        // Source file
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
        
        // Test file
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
        
        // Test output file
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
        
        // Coverage report
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
        
        // Project root
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
        
        // Included files
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
        
        // Test command
        Label testCommandLabel = new Label(commandGroup, SWT.NONE);
        testCommandLabel.setText("Test Command:");
        
        testCommandText = new Text(commandGroup, SWT.BORDER);
        testCommandText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        // Test command directory
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
        
        // Model
        Label modelLabel = new Label(commandGroup, SWT.NONE);
        modelLabel.setText("Model:");
        
        modelCombo = new Combo(commandGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        modelCombo.setItems(new String[] {
        	"deepseek/deepseek-chat-v3-0324:free",
        	"deepseek/deepseek-chat:free",
        	"google/gemini-2.5-pro-exp-03-25:free",
            
            
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
        
        // Coverage type
        Label coverageTypeLabel = new Label(coverageGroup, SWT.NONE);
        coverageTypeLabel.setText("Coverage Type:");
        
        coverageTypeCombo = new Combo(coverageGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        coverageTypeCombo.setItems(new String[] {"jacoco"});
        coverageTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        // Desired coverage
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
        
        // Max iterations
        Label maxIterationsLabel = new Label(advancedGroup, SWT.NONE);
        maxIterationsLabel.setText("Max Iterations:");
        
        maxIterationsSpinner = new Spinner(advancedGroup, SWT.BORDER);
        maxIterationsSpinner.setMinimum(1);
        maxIterationsSpinner.setMaximum(100);
        maxIterationsSpinner.setIncrement(1);
        maxIterationsSpinner.setPageIncrement(5);
        maxIterationsSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        // Run each test separately
        runEachTestSeparatelyCheckbox = new Button(advancedGroup, SWT.CHECK);
        runEachTestSeparatelyCheckbox.setText("Run Each Test Separately");
        runEachTestSeparatelyCheckbox.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        
        // Run tests multiple times
        Label runTestsMultipleTimesLabel = new Label(advancedGroup, SWT.NONE);
        runTestsMultipleTimesLabel.setText("Run Tests Multiple Times:");
        
        runTestsMultipleTimesSpinner = new Spinner(advancedGroup, SWT.BORDER);
        runTestsMultipleTimesSpinner.setMinimum(1);
        runTestsMultipleTimesSpinner.setMaximum(10);
        runTestsMultipleTimesSpinner.setIncrement(1);
        runTestsMultipleTimesSpinner.setPageIncrement(1);
        runTestsMultipleTimesSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        
        // Additional instructions
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
        
        // Make sure the group takes full width and appropriate height
        GridData apiGroupData = new GridData(SWT.FILL, SWT.FILL, true, false);
        apiGroupData.horizontalSpan = 1;  // Span the full width of the container
        apiGroup.setLayoutData(apiGroupData);

        // API key - Make this more prominent
        Label apiKeyLabel = new Label(apiGroup, SWT.NONE);
        apiKeyLabel.setText("OpenRouter API Key (Required):");
        
        apiKeyText = new Text(apiGroup, SWT.BORDER | SWT.PASSWORD);
        apiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Add a note about getting an API key
        Label apiKeyNoteLabel = new Label(apiGroup, SWT.NONE);
        apiKeyNoteLabel.setText("Get an API key from openrouter.ai");
        apiKeyNoteLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        // Site URL
        Label siteUrlLabel = new Label(apiGroup, SWT.NONE);
        siteUrlLabel.setText("Site URL:");
        
        siteUrlText = new Text(apiGroup, SWT.BORDER);
        siteUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Site name
        Label siteNameLabel = new Label(apiGroup, SWT.NONE);
        siteNameLabel.setText("Site Name:");
        
        siteNameText = new Text(apiGroup, SWT.BORDER);
        siteNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    /**
     * Initialize the dialog with default values
     */
    private void initializeValues() {
        // Set default values
        if (javaProject != null) {
            IProject project = javaProject.getProject();
            projectRootText.setText(project.getLocation().toOSString());
            testCommandDirText.setText(project.getLocation().toOSString());
            
            // Set default test command based on project type
            if (isMavenProject(project)) {
                testCommandText.setText("mvn -f \"" + project.getLocation().toOSString() + "\" clean test");
            } else if (isGradleProject(project)) {
                testCommandText.setText("./gradlew test");
            }
            
            // Set default coverage report path
            if (isMavenProject(project)) {
                coverageReportText.setText(project.getLocation().append("target/site/jacoco/jacoco.xml").toOSString());
            } else if (isGradleProject(project)) {
                coverageReportText.setText(project.getLocation().append("build/reports/jacoco/test/jacocoTestReport.xml").toOSString());
            }
            
            // Set default values for API configuration
            siteUrlText.setText("http://localhost");
            siteNameText.setText("EclipseCoverAgentPlugin");
            
            // If a file is selected, set it as the source file
            if (selectedFile != null) {
            	  logger.info("initializeValues: selectedFile is NOT null. Attempting to set sourceFileText.");
                  try {
                      String path = selectedFile.getLocation().toOSString();
                      logger.info("initializeValues: Path to set: " + path);
                      if (sourceFileText == null) {
                           logger.severe("initializeValues: sourceFileText is NULL before setting text!");
                      } else {
                           sourceFileText.setText(path);
                           logger.info("initializeValues: Successfully called sourceFileText.setText().");
                           // Optionally check if it worked immediately:
                            logger.info("initializeValues: sourceFileText.getText() after set: " + sourceFileText.getText());
                      }
                  } catch (Exception ex) {
                      logger.log(java.util.logging.Level.SEVERE, "initializeValues: Error setting sourceFileText", ex);
                 }
                sourceFileText.setText(selectedFile.getLocation().toOSString());
                
                // Try to find a corresponding test file
                String fileName = selectedFile.getName();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                
                try {
                    IFolder testFolder = project.getFolder("src/test/java");
                    if (testFolder.exists()) {
                        findTestFile(testFolder, baseName + "Test.java");
                    }
                } catch (CoreException e) {
                    logger.warning("Error finding test folder: " + e.getMessage());
                }
            }
            String savedApiKey = SecureStorageUtil.getApiKey();
            if (savedApiKey != null && !savedApiKey.isEmpty()) {
                apiKeyText.setText(savedApiKey);
                logger.info("Loaded API key from secure storage");
            }
        }
        
        // Set default values for other fields
        modelCombo.select(0);
        coverageTypeCombo.select(0);
        desiredCoverageSpinner.setSelection(80);
        maxIterationsSpinner.setSelection(2);
        runTestsMultipleTimesSpinner.setSelection(1);
        siteUrlText.setText("http://localhost");
        siteNameText.setText("EclipseCoverAgentPlugin");
        
        // Load Java files from the project
        loadJavaFiles();
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
     * Load Java files from the project and populate the included files viewer
     */
    private void loadJavaFiles() {
        try {
            IPackageFragmentRoot[] packageRoots = javaProject.getPackageFragmentRoots();
            for (IPackageFragmentRoot root : packageRoots) {
                if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
                    IResource resource = root.getCorrespondingResource();
                    if (resource instanceof IFolder) {
                        try {
							loadJavaFilesFromFolder((IFolder) resource);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                    }
                }
            }
        } catch (JavaModelException e) {
            logger.warning("Error loading Java files: " + e.getMessage());
        }
    }

    /**
     * Recursively load Java files from the given folder
     *
     * @param folder The folder to search
     * @throws CoreException If an error occurs while accessing the folder
     */
    private void loadJavaFilesFromFolder(IFolder folder) throws CoreException {
        for (IResource resource : folder.members()) {
            if (resource instanceof IFile && "java".equals(resource.getFileExtension())) {
                includedFilePaths.add(resource.getLocation().toOSString());
            } else if (resource instanceof IFolder) {
                loadJavaFilesFromFolder((IFolder) resource);
            }
        }
        includedFilesViewer.setInput(includedFilePaths);
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
     * Add a file to the included files list
     */
    private void addIncludedFile() {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setText("Select Included File");
        dialog.setFilterExtensions(new String[]{"*.java"});
        String selectedFile = dialog.open();
        if (selectedFile != null) {
            includedFilePaths.add(selectedFile);
            includedFilesViewer.setInput(includedFilePaths);
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
            // --- Start Validation ---
            setErrorMessage(null); // Clear previous error messages

            String sourceFilePath = sourceFileText.getText().trim();
            if (sourceFilePath.isEmpty()) {
                setErrorMessage("Source File path cannot be empty.");
                sourceFileText.setFocus(); // Optional: set focus to the problematic field
                return; // Stop processing, keep dialog open
            }
            if (!Files.exists(Paths.get(sourceFilePath))) {
                setErrorMessage("Source File does not exist: " + sourceFilePath);
                sourceFileText.setFocus();
                return; // Stop processing, keep dialog open
            }

            String testFilePath = testFileText.getText().trim();
             // Add similar checks for test file if it's mandatory or needs validation
            if (testFilePath.isEmpty()) {
                 setErrorMessage("Test File path cannot be empty.");
                 testFileText.setFocus();
                 return;
            }
             // Optionally check if test file exists if it MUST exist beforehand
//            if (!Files.exists(Paths.get(testFilePath))) {
//                setErrorMessage("Test File does not exist: " + testFilePath);
//                testFileText.setFocus();
//                return;
//            }


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

            // Check if API key is provided (already present, but good to keep)
            String apiKey = apiKeyText.getText().trim();
            if (apiKey.isEmpty()) {
                // Use setErrorMessage instead of a popup for consistency
                setErrorMessage("OpenRouter API key is required. Please enter a valid API key from openrouter.ai");
                apiKeyText.setFocus(); // Set focus to the API key field
                return; // Stop processing, keep dialog open
            }
            // Save the API key only if validation passes this far
            SecureStorageUtil.saveApiKey(apiKey);
            logger.info("API Key validated and saved to secure storage.");
  
        
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
                .useReportCoverageFeatureFlag(useReportCoverageCheckbox.getSelection())
                .projectRoot(projectRootText.getText())
                .maxIterations(maxIterationsSpinner.getSelection())
                .diffCoverage(diffCoverageCheckbox.getSelection())
                .strictCoverage(strictCoverageCheckbox.getSelection())
                .runEachTestSeparately(runEachTestSeparatelyCheckbox.getSelection())
                .runTestsMultipleTimes(runTestsMultipleTimesSpinner.getSelection())
                .apiKey(apiKeyText.getText())
                .siteUrl(siteUrlText.getText())
                .siteName(siteNameText.getText())
                .build();

     // --- Schedule the Job ---
        Job job = new RunCoverAgentJob("Running CoverAgent", args);
        job.schedule(); // Schedule the job to run in the background

        logger.info("CoverAgent job scheduled. Dialog will close.");


        super.okPressed();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Run", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }
}