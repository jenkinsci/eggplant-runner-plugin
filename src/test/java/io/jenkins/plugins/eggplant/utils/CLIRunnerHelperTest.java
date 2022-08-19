package io.jenkins.plugins.eggplant.utils;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.FilePath;
import io.jenkins.plugins.eggplant.common.OperatingSystem;
import io.jenkins.plugins.eggplant.exception.InvalidRunnerException;

public class CLIRunnerHelperTest {
    
    private File tempDir;
    private CLIRunnerHelper helper;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
	public void beforeTest() throws IOException, InterruptedException {
        tempDir = Files.createTempDirectory(null).toFile();
        System.setOut(new PrintStream(outContent));
        helper = new CLIRunnerHelper(new FilePath(tempDir), OperatingSystem.WINDOWS, System.out);
	}

	@After
	public void afterTest() throws IOException {
       FileUtils.deleteDirectory(tempDir);
       
       System.setOut(System.out);
	}

    @Test
    public void testDowloadRunner() throws Exception {

        //first download
        helper.downloadRunner(null);
        assertTrue(outContent.toString().contains("Download successfully"));
        
        //second download - skip download
        helper.downloadRunner(null);
        assertTrue(outContent.toString().contains("Runner found in default directory, skipping download."));

    }

    @Test
    public void testCopyRunnerFromPath() throws Exception {

        // Path does not exist
        InvalidRunnerException thrown = assertThrows(InvalidRunnerException.class, ()->{
            helper.copyRunnerFrom("a path");
        });
        assertTrue(thrown.getMessage().contains("No such file or permission denied."));   

        Path userPath = Files.createDirectory(tempDir.toPath().resolve("user"));

        // path exist but mismatch filename
        thrown = assertThrows(InvalidRunnerException.class, ()->{
            Path filepath = userPath.resolve("eggplant-runner.exe");
            Files.createFile(filepath);
            helper.copyRunnerFrom(filepath.toString());
            });
        assertTrue(thrown.getMessage().contains("File found is invalid. Required: "+helper.getFilename()+". Please download from " + helper.getPublicDownloadLink()));
        
        // path exist and valid
        Path cliPath = userPath.resolve(helper.getFilename());
        Files.createFile(cliPath);
        helper.copyRunnerFrom(cliPath.toString());
        assertTrue(outContent.toString().contains("Fetch complete."));

    }

}
