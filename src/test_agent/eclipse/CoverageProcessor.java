package test_agent.eclipse;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * CoverageProcessor processes a JaCoCo XML coverage report for a specific source file.
 * It verifies that the report exists and is up-to-date and then parses the XML to extract
 * the covered and missed line numbers and computes the overall coverage percentage.
 */
public class CoverageProcessor {

    private String jacocoReportPath;
    private String srcFilePath;
    private static final Logger logger = Logger.getLogger(CoverageProcessor.class.getName());

    /**
     * Constructor.
     *
     * @param jacocoReportPath                    the path to the coverage report file.
     * @param srcFilePath                  the fully qualified path of the source file.
     */
    public CoverageProcessor(String jacocoReportPath, String srcFilePath
                            ) {
        this.jacocoReportPath = jacocoReportPath;
        this.srcFilePath = srcFilePath;
       // logger.info("CoverageProcessor initialized with srcFilePath: " + srcFilePath);

    }


    /**
     * Processes the coverage report by verifying its update time and then parsing it.
     *
     * @param timeOfTestCommand the time  when the test command was run.
     * @return a CoverageData object containing the covered lines, missed lines, and coverage percentage.
     */
    public CoverageData processCoverageReport(long timeOfTestCommand) {
        verifyReportUpdate(timeOfTestCommand);
        return parseCoverageReportJacoco();
    }

    /**
     * Verifies that the coverage report file exists and was updated after the test command.
     *
     * @param timeOfTestCommand the time  when the test command was run.
     * @throws RuntimeException if the report file does not exist.
     */
    private void verifyReportUpdate(long timeOfTestCommand) {
        File reportFile = new File(jacocoReportPath);
        if (!reportFile.exists()) {
            throw new RuntimeException("Fatal: Coverage report \"" + jacocoReportPath + "\" was not generated.");
        }
        long fileModTimeMs = reportFile.lastModified();
        if (fileModTimeMs <= timeOfTestCommand) {
            logger.warning("The coverage report file was not updated after the test command. " +
                    "fileModTimeMs: " + fileModTimeMs + ", timeOfTestCommand: " + timeOfTestCommand);
        }
    }



    /**
     * Parses a JaCoCo XML report to extract the coverage for a specific source file.
     *
     * @return the coverage data.
     */
    private CoverageData parseCoverageReportJacoco() {
        //logger.info("parseCoverageReportJacoco called with srcFilePath: " + srcFilePath);

        // Determine source file extension and extract package/class if it's a Java file.
        String srcFileExtension = getFileExtension(srcFilePath);
        String packageName = "";
        String className = "";
        if ("java".equalsIgnoreCase(srcFileExtension)) {
            String[] packageAndClass = extractPackageAndClassJava(srcFilePath);
            packageName = packageAndClass[0];
            className = packageAndClass[1];
        } else {
            logger.warning("Unsupported source file extension: " + srcFileExtension +
                    ". Defaulting to Java extraction.");
            String[] packageAndClass = extractPackageAndClassJava(srcFilePath);
            packageName = packageAndClass[0];
            className = packageAndClass[1];
        }

        // Parse the JaCoCo XML report to get line-level data for the class.
        LineData lineData = parseMissedCoveredLinesJacocoXml(className);
        int totalLines = lineData.covered.size() + lineData.missed.size();
        double coveragePercentage = (totalLines > 0) ? ((double) lineData.covered.size() / totalLines) : 0.0;

        // Return the final coverage results.
        return new CoverageData(lineData.covered, lineData.missed, coveragePercentage);
    }

    /**
     * Returns the file extension for a given filename.
     *
     * @param filename the filename.
     * @return the extension (without the dot), or an empty string if none found.
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }

    /**
     * Extracts the package and class name from a Java source file.
     *
     * @param srcFilePath the path to the Java source file.
     * @return a String array where index 0 is the package name and index 1 is the class name.
     */
    private String[] extractPackageAndClassJava(String srcFilePath) {
        String packageName = "";
        String className = "";
        Pattern packagePattern = Pattern.compile("^\\s*package\\s+([\\w\\.]+)\\s*;.*$");
        Pattern classPattern = Pattern.compile("^\\s*public\\s+class\\s+(\\w+).*");

        try (BufferedReader reader = new BufferedReader(new FileReader(srcFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (packageName.isEmpty()) {
                    Matcher packageMatcher = packagePattern.matcher(line);
                    if (packageMatcher.matches()) {
                        packageName = packageMatcher.group(1);
                    }
                }
                if (className.isEmpty()) {
                    Matcher classMatcher = classPattern.matcher(line);
                    if (classMatcher.matches()) {
                        className = classMatcher.group(1);
                    }
                }
                if (!packageName.isEmpty() && !className.isEmpty()) {
                    break;
                }
            }
        } catch (IOException e) {
            logger.severe("Error reading file " + srcFilePath + ": " + e.getMessage());
            throw new RuntimeException(e);
        }

        return new String[]{packageName, className};
    }

    /**
     * Parses a JaCoCo XML report to extract the covered and missed line numbers for the specified class.
     *
     * @param className the class name to look for (without extension).
     * @return a LineData object containing lists of covered and missed lines.
     */
    private LineData parseMissedCoveredLinesJacocoXml(String className) {
        List<Integer> coveredLines = new ArrayList<>();
        List<Integer> missedLines = new ArrayList<>();
        try {
            File xmlFile = new File(jacocoReportPath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Disable external DTD loading to avoid missing DTD file issues.
            try {
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (ParserConfigurationException e) {
                logger.warning("Could not disable external DTD loading: " + e.getMessage());
            }

            DocumentBuilder builder = factory.newDocumentBuilder();
            // Provide a custom EntityResolver that ignores external DTD references.
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Look for the <sourcefile> element with name equal to className + ".java"
            NodeList sourceFiles = doc.getElementsByTagName("sourcefile");
            Element matchingSourceFile = null;
            for (int i = 0; i < sourceFiles.getLength(); i++) {
                Node node = sourceFiles.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String nameAttr = element.getAttribute("name");
                    if (nameAttr.equals(className + ".java")) {
                        matchingSourceFile = element;
                        break;
                    }
                }
            }

            if (matchingSourceFile == null) {
                logger.warning("No matching <sourcefile> element found for class: " + className);
                return new LineData(coveredLines, missedLines);
            }

            NodeList lineNodes = matchingSourceFile.getElementsByTagName("line");
            for (int i = 0; i < lineNodes.getLength(); i++) {
                Node lineNode = lineNodes.item(i);
                if (lineNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element lineElement = (Element) lineNode;
                    String nrAttr = lineElement.getAttribute("nr");
                    String miAttr = lineElement.getAttribute("mi");
                    int lineNumber = Integer.parseInt(nrAttr);
                    // If missed instructions ("mi") is "0", consider the line as covered.
                    if ("0".equals(miAttr)) {
                        coveredLines.add(lineNumber);
                    } else {
                        missedLines.add(lineNumber);
                    }
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.severe("Error parsing XML file " + jacocoReportPath + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
        return new LineData(coveredLines, missedLines);
    }


    /**
     * A simple container to hold lists of covered and missed line numbers.
     */
    private static class LineData {
        List<Integer> covered;
        List<Integer> missed;

        public LineData(List<Integer> covered, List<Integer> missed) {
            this.covered = covered;
            this.missed = missed;
        }
    }

    /**
     * A data class representing the final coverage results.
     */
    public static class CoverageData {
        private List<Integer> coveredLines;
        private List<Integer> missedLines;
        private double coveragePercentage;

        public CoverageData(List<Integer> coveredLines, List<Integer> missedLines, double coveragePercentage) {
            this.coveredLines = coveredLines;
            this.missedLines = missedLines;
            this.coveragePercentage = coveragePercentage;
        }

        public List<Integer> getCoveredLines() {
            return coveredLines;
        }

        public List<Integer> getMissedLines() {
            return missedLines;
        }

        public double getCoveragePercentage() {
            return coveragePercentage;
        }

        @Override
        public String toString() {
            return "CoverageData{" +
                    "coveredLines=" + coveredLines +
                    ", missedLines=" + missedLines +
                    ", coveragePercentage=" + coveragePercentage +
                    '}';
        }
    }

}

