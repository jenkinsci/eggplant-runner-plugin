package io.jenkins.plugins.eggplant.utils;

import hudson.FilePath;
import io.jenkins.plugins.eggplant.common.OperatingSystem;
import io.jenkins.plugins.eggplant.exception.InvalidRunnerException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class CLIRunnerHelperTest {

    private File tempDir;
    private CLIRunnerHelper helper;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        tempDir = Files.createTempDirectory(null).toFile();
        System.setOut(new PrintStream(outContent));
        helper = new CLIRunnerHelper(new FilePath(tempDir), OperatingSystem.WINDOWS, System.out);
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(tempDir);

        System.setOut(System.out);
    }

    @Test
    void testDownloadRunner() throws Exception {
        //first download
        helper.downloadRunner(null);
        assertTrue(outContent.toString().contains("Download successfully"));

        //second download - skip download
        helper.downloadRunner(null);
        assertTrue(outContent.toString().contains("Runner found in default directory, skipping download."));
    }

    @Test
    void testCopyRunnerFromPath() throws Exception {
        // Path does not exist
        InvalidRunnerException thrown = assertThrows(InvalidRunnerException.class, () ->
                helper.copyRunnerFrom("a path"));
        assertTrue(thrown.getMessage().contains("No such file or permission denied."));

        Path userPath = Files.createDirectory(tempDir.toPath().resolve("user"));

        // path exists but mismatch filename
        thrown = assertThrows(InvalidRunnerException.class, () -> {
            Path filepath = userPath.resolve("eggplant-runner.exe");
            Files.createFile(filepath);
            helper.copyRunnerFrom(filepath.toString());
        });
        assertTrue(thrown.getMessage().contains("File found is invalid. Required: " + helper.getFilename() + ". Please download from " + helper.getPublicDownloadLink()));

        // path exists and valid
        Path cliPath = userPath.resolve(helper.getFilename());
        Files.createFile(cliPath);
        helper.copyRunnerFrom(cliPath.toString());
        assertTrue(outContent.toString().contains("Fetch complete."));
    }

}
