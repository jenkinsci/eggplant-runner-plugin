package io.jenkins.plugins.eggplant;

import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.Secret;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EggplantRunnerBuilder extends Builder implements SimpleBuildStep {
    private final static String CLI_VERSION = "6.1";
    private final static String CLI_DOWNLOAD_URL = "https://downloads.eggplantsoftware.com/downloads/EggplantRunner/${cliFilename}";
    private final static Map<OperatingSystem, String> CLI_FILENAME = Stream.of(
        new AbstractMap.SimpleEntry<>(OperatingSystem.LINUX, "eggplant-runner-Linux-${cliVersion}-ci"),
        new AbstractMap.SimpleEntry<>(OperatingSystem.MACOS, "eggplant-runner-MacOS-${cliVersion}-ci"), 
        new AbstractMap.SimpleEntry<>(OperatingSystem.WINDOWS, "eggplant-runner-Windows-${cliVersion}-ci.exe")
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private String serverURL;
    private String testConfigId;
    private String clientId;
    private Secret clientSecret;
    private String logLevel;
    private String caCertPath;
    private String pollInterval;
    private String requestTimeout;
    private String requestRetries;

    @DataBoundConstructor
    public EggplantRunnerBuilder() {
    }

    public String getServerURL() {
        return serverURL;
    }
    public String getTestConfigId() {
        return testConfigId;
    }
    public String getClientId() {
        return clientId;
    }
    public Secret getClientSecret() {
        return clientSecret;
    }
    public String getLogLevel() {
        return logLevel;
    }
    public String getCaCertPath() {
        return caCertPath;
    }
    public String getPollInterval() {
        return pollInterval;
    }
    public String getRequestTimeout() {
        return requestTimeout;
    }
    public String getRequestRetries() {
        return requestRetries;
    }

    @DataBoundSetter
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    @DataBoundSetter
    public void setTestConfigId(String testConfigId) {
        this.testConfigId = testConfigId;
    }

    @DataBoundSetter
    public void setClientId(String clientId) {
        this.clientId= clientId;
    }

    
    @DataBoundSetter
    public void setClientSecret(Secret clientSecret) {
        this.clientSecret = clientSecret;
    }

    @DataBoundSetter
    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }
    @DataBoundSetter
    public void setCaCertPath(String caCertPath) {
        this.caCertPath = caCertPath;
    }
    @DataBoundSetter
    public void setPollInterval(String pollInterval) {
        this.pollInterval = pollInterval;
    }
    @DataBoundSetter
    public void setRequestTimeout(String requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
    @DataBoundSetter
    public void setRequestRetries(String requestRetries) {
        this.requestRetries = requestRetries;
    }
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        String buildId = run.getId();
        Locale locale = Locale.getDefault();
        String localeString = String.format("%s.utf-8", locale.toString());

        OperatingSystem os = this.getOperatingSystem(workspace, launcher);
        FilePath uniqueWorkspace = workspace.child(buildId); 
        FilePath cliFile = this.downloadCLIExecutable(uniqueWorkspace, os);
        logger.println("cliFile: " + cliFile);
        String[] command = this.getCommand(cliFile, buildId, os);
        logger.println("command: " + command);
        EnvVars envVars = new EnvVars();
        envVars.put("LC_ALL", localeString);
        envVars.put("LANG", localeString);

        ProcStarter procStarter = launcher.launch();
        Proc process = procStarter.pwd(uniqueWorkspace).cmds(command).envs(envVars).quiet(false).stderr(logger).stdout(logger).start();
        int exitCode = process.join();
        if (exitCode != 0) throw new EggplantRunnerExitException(exitCode);
    }

    private FilePath downloadCLIExecutable(FilePath workspace, OperatingSystem os) throws IOException, InterruptedException {
        String cliFilename = CLI_FILENAME.get(os).replace("${cliVersion}", CLI_VERSION);
        String cliDownloadUrl = CLI_DOWNLOAD_URL.replace("${cliFilename}", cliFilename);
        InputStream in;

        // It will only use gitlab package registry if no gitlab Access token (for development)
        if (System.getenv("gitlabAccessToken") == null) 
            in = new URL(cliDownloadUrl).openStream();
        else
        {
            URL url = new URL("https://gitlab.com/api/v4/projects/22402994/packages/generic/6.1-ci-cd/0.0.0/" + cliFilename);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty ("PRIVATE-TOKEN", System.getenv("gitlabAccessToken"));
            connection.setDoOutput(true);
            in = connection.getInputStream();
        }

        FilePath filePath = workspace.child(cliFilename);
        filePath.copyFrom(in);
        filePath.chmod(0755);
        return filePath;
    }

    private OperatingSystem getOperatingSystem(FilePath workspace, Launcher launcher) throws IOException, InterruptedException {
        
        // If not Unix, then it is Windows
        if (!launcher.isUnix()) 
            return OperatingSystem.WINDOWS;

        ProcStarter procStarter = launcher.launch();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Proc process = procStarter.pwd(workspace).cmds("uname").quiet(true).stdout(outputStream).start();
        process.join();
        String output = outputStream.toString("UTF-8");

        // MacOS
        if (output.startsWith("Darwin")) 
            return OperatingSystem.MACOS;
        
        // Linux
        return OperatingSystem.LINUX;
      }
    

    private String[] getCommand(FilePath cliFile, String buildId, OperatingSystem os) {
        List<String> commandList = new ArrayList<String>();
        
        //commandList.add("./" + cliFile.getName()); // cliPath
        commandList.add(cliFile.getRemote()); // cliPath
        commandList.add(this.serverURL); // serverURLArg
        commandList.add(this.testConfigId); // testConfigIdArgs
        if (!this.clientId.equals("")) // clientIdArg
            commandList.add(String.format("--client-id=%s", this.clientId)); 
        commandList.add(String.format("--client-secret=%s", this.clientSecret)); // clientSecretArg

        if (!this.logLevel.equals("")) // logLevelArg
            commandList.add(String.format("--log-level=%s", this.logLevel)); 
        if (!this.caCertPath.equals("")) // caCertPathArg
            commandList.add(String.format("--ca-cert-path=%s", this.caCertPath)); 
        if (!this.pollInterval.equals("")) // caCertPathArg
            commandList.add(String.format("--poll-interval=%s", this.pollInterval)); 
        if (!this.requestTimeout.equals("")) // requestTimeoutArg
            commandList.add(String.format("--request-timeout=%s", this.requestTimeout)); 
        if (!this.requestRetries.equals("")) // requestTimeoutArg
            commandList.add(String.format("--request-retries=%s", this.requestRetries)); 

        return commandList.toArray(new String[0]);
    }

    @Symbol("eggplantRunner")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Eggplant Runner";
        }

    }

}
