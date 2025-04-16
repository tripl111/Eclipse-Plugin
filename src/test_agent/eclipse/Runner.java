package test_agent.eclipse;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for executing shell commands with timeout functionality.
 */
public class Runner {
    // Default timeout of 1 hour (3600 seconds)
    private static final int MAX_ALLOWED_RUNTIME_SECONDS = 3600;

    /**
     * Result class to hold the command execution results
     */
    public static class CommandResult {
        private final String stdout;
        private final String stderr;
        private final int exitCode;
        private final long commandStartTime;

        public CommandResult(String stdout, String stderr, int exitCode, long commandStartTime) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
            this.commandStartTime = commandStartTime;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public int getExitCode() {
            return exitCode;
        }

        public long getCommandStartTime() {
            return commandStartTime;
        }
    }

    /**
     * Executes a shell command in a specified working directory and returns its output, error, and exit code.
     *
     * @param command The shell command to execute
     * @param cwd     The working directory in which to execute the command (optional)
     * @return CommandResult containing stdout, stderr, exit code, and command start time
     */
    public static CommandResult runCommand(String command, String cwd) {
        // Get the current time in milliseconds
        long commandStartTime = System.currentTimeMillis();

        ProcessBuilder processBuilder = new ProcessBuilder();
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }

        if (cwd != null) {
            processBuilder.directory(new File(cwd));
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        try {
            Process process = processBuilder.start();

            // Handle stdout in a separate thread
            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append(System.lineSeparator());
                    }
                } catch (Exception e) {
                    stderr.append("Error reading stdout: ").append(e.getMessage());
                }
            });

            // Handle stderr in a separate thread
            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append(System.lineSeparator());
                    }
                } catch (Exception e) {
                    stderr.append("Error reading stderr: ").append(e.getMessage());
                }
            });

            // Start both threads
            stdoutThread.start();
            stderrThread.start();

            // Wait for the process to complete or timeout
            boolean completed = process.waitFor(MAX_ALLOWED_RUNTIME_SECONDS, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                return new CommandResult(
                        "",
                        "Command timed out after " + MAX_ALLOWED_RUNTIME_SECONDS + " seconds",
                        -1,
                        commandStartTime
                );
            }

            // Wait for the output threads to complete
            stdoutThread.join();
            stderrThread.join();

            return new CommandResult(
                    stdout.toString(),
                    stderr.toString(),
                    process.exitValue(),
                    commandStartTime
            );

        } catch (Exception e) {
            return new CommandResult(
                    "",
                    "Error executing command: " + e.getMessage(),
                    -1,
                    commandStartTime
            );
        }
    }
}
