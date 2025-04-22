package test_agent.eclipse.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to find source file dependencies based on Java imports
 * within the same project.
 */
public class DependencyResolver {

    private static final Logger logger = Logger.getLogger(DependencyResolver.class.getName());

    /**
     * Collects source file dependencies (IFile) for a given compilation unit
     * by resolving its import statements to other source files within the same project.
     *
     * @param compilationUnit The ICompilationUnit to analyze.
     * @return A List of IFile objects representing the source files of the resolved imports.
     *         Returns an empty list if the input is null, doesn't exist, or on error.
     */
    public static List<IFile> collect(ICompilationUnit compilationUnit) {
        // Use a Set initially to automatically handle duplicates
        Set<IFile> dependencyFiles = new HashSet<>();

        if (compilationUnit == null || !compilationUnit.exists()) {
            logger.warning("Cannot resolve dependencies for null or non-existent compilation unit.");
            return new ArrayList<>();
        }

        IJavaProject javaProject = compilationUnit.getJavaProject();
        if (javaProject == null) {
             logger.warning("Cannot get Java project from compilation unit: " + compilationUnit.getElementName());
             return new ArrayList<>();
        }

        logger.info("Resolving dependencies for: " + compilationUnit.getElementName());

        try {
            IImportDeclaration[] imports = compilationUnit.getImports();
            if (imports == null || imports.length == 0) {
                logger.info("No import declarations found in " + compilationUnit.getElementName());
                return new ArrayList<>();
            }

            for (IImportDeclaration imp : imports) {
                String importName = imp.getElementName();

                // Basic check to skip wildcard imports (more complex resolution needed for these)
                // We focus on specific type imports as they are direct dependencies.
                if (importName.endsWith(".*")) {
                    logger.finer("Skipping wildcard import: " + importName);
                    continue;
                }

                // Resolve the type using the project's classpath
                // This is the core JDT call to find where the imported type comes from.
                IType importedType = javaProject.findType(importName);

                if (importedType != null && importedType.exists()) {
                    // Check if the type comes from *source code* within the project
                    ICompilationUnit importedCU = importedType.getCompilationUnit();
                    if (importedCU != null && importedCU.exists()) {
                        // Get the underlying file resource
                        IResource resource = importedCU.getResource();
                        if (resource instanceof IFile) {
                            IFile file = (IFile) resource;
                            // Exclude the file being analyzed itself if it somehow gets resolved
                            if (!file.equals(compilationUnit.getResource())) {
                                logger.fine("Resolved import " + importName + " to project source file: " + file.getFullPath());
                                dependencyFiles.add(file);
                            }
                        } else {
                             logger.finer("Resolved import " + importName + " to a resource that is not an IFile: " + (resource != null ? resource.getFullPath() : "null"));
                        }
                    } else {
                        // Type found, but not from project source (e.g., from a JAR/binary dependency)
                        logger.finer("Import " + importName + " resolved to a non-project source (binary/library). Skipping inclusion.");
                    }
                } else {
                    // Type not found - could be an error in the source code,
                    // or the type isn't on the classpath.
                    logger.warning("Could not resolve import: " + importName + " within project " + javaProject.getElementName());
                }
            }

        } catch (JavaModelException e) {
            // Log exceptions during JDT model access
            logger.log(Level.SEVERE, "Error resolving dependencies for " + compilationUnit.getElementName(), e);
        } catch (Exception e) {
            // Catch unexpected errors
             logger.log(Level.SEVERE, "Unexpected error during dependency resolution for " + compilationUnit.getElementName(), e);
        }

        logger.info("Found " + dependencyFiles.size() + " unique project source dependencies for " + compilationUnit.getElementName());
        // Convert the Set back to a List for the return type
        return new ArrayList<>(dependencyFiles);
    }
}