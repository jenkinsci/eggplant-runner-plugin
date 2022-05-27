package io.jenkins.plugins.eggplant;

import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.Secret;
import io.jenkins.cli.shaded.org.apache.commons.lang.LocaleUtils;
import io.jenkins.plugins.eggplant.common.LogLevel;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import jenkins.tasks.SimpleBuildStep;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class EggplantRunnerBuilder extends Builder implements SimpleBuildStep {
    private final static String CLI_VERSION = "6.1.2-1";
    private final static String CLI_DOWNLOAD_URL = "https://downloads.eggplantsoftware.com/downloads/EggplantRunner/${cliFilename}";
    private final static Map<OperatingSystem, String> CLI_FILENAME = Stream.of(
        new AbstractMap.SimpleEntry<>(OperatingSystem.LINUX, "eggplant-runner-Linux-${cliVersion}"),
        new AbstractMap.SimpleEntry<>(OperatingSystem.MACOS, "eggplant-runner-MacOS-${cliVersion}"), 
        new AbstractMap.SimpleEntry<>(OperatingSystem.WINDOWS, "eggplant-runner-Windows-${cliVersion}.exe")
    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    private String serverURL;
    private String testConfigId;
    private String clientId;
    private Secret clientSecret;
    private LogLevel logLevel;
    private String CACertPath;
    private String pollInterval;
    private String requestTimeout;
    private String requestRetries;
    private Boolean dryRun;
    private String backoffFactor;

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
    public LogLevel getLogLevel() {
        return logLevel;
    }
    public String getCACertPath() {
        return CACertPath;
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
    public String getBackoffFactor() {
        return backoffFactor;
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
    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }
    @DataBoundSetter
    public void setCACertPath(String CACertPath) {
        this.CACertPath = CACertPath;
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
    @DataBoundSetter
    public void setBackoffFactor(String backoffFactor) {
        this.backoffFactor = backoffFactor;
    }    
    @DataBoundSetter
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        String buildId = run.getId();
        String localeString = "";
        Locale locale = Locale.getDefault();

        logger.println("locale: " + locale);
        logger.println("locale.getCountry(): " + locale.getCountry());
        if (!locale.getCountry().equals(""))
            localeString = String.format("%s.utf-8", locale.toString());
        else
            localeString = String.format("%s.utf-8", "en_US");

        OperatingSystem os = this.getOperatingSystem(workspace, launcher);
        FilePath uniqueWorkspace = workspace.child(buildId); 
        FilePath cliFile = this.downloadCLIExecutable(uniqueWorkspace, os);
        logger.println("cliFile: " + cliFile);
        String[] command = this.getCommand(cliFile, buildId, os, env);
        logger.println("command: " + Arrays.toString(command));
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
            URL url = new URL("https://gitlab.com/api/v4/projects/22402994/packages/generic/6.1.2-1/0.0.0/" + cliFilename);
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
    

    private String[] getCommand(FilePath cliFile, String buildId, OperatingSystem os, EnvVars env) {
        List<String> commandList = new ArrayList<String>();
        
        //commandList.add("./" + cliFile.getName()); // cliPath
        commandList.add(cliFile.getRemote()); // cliPath
        commandList.add(this.serverURL); // serverURLArg
        commandList.add(this.testConfigId); // testConfigIdArgs
        if (this.clientId != null && !this.clientId.equals("")) // clientIdArg
            commandList.add(String.format("--client-id=%s", this.clientId)); 
        
        // clientSecretArg
        if (this.clientSecret != null && !this.clientSecret.getPlainText().isEmpty()) 
            commandList.add(String.format("--client-secret=%s", this.clientSecret));
        else if (env.get("DAI_CLIENT_SECRET") != null && !env.get("DAI_CLIENT_SECRET").equals("")) 
            commandList.add(String.format("--client-secret=%s", env.get("DAI_CLIENT_SECRET")));

        if (this.logLevel != null) // logLevelArg
            commandList.add(String.format("--log-level=%s", this.logLevel)); 
        if (this.CACertPath != null && !this.CACertPath.equals("")) // CACertPathArg
            commandList.add(String.format("--ca-cert-path=%s", this.CACertPath)); 
        if (this.pollInterval != null && !this.pollInterval.equals("")) // CACertPathArg
            commandList.add(String.format("--poll-interval=%s", this.pollInterval)); 
        if (this.requestTimeout != null && !this.requestTimeout.equals("")) // requestTimeoutArg
            commandList.add(String.format("--request-timeout=%s", this.requestTimeout)); 
        if (this.requestRetries != null && !this.requestRetries.equals("")) // requestTimeoutArg
            commandList.add(String.format("--request-retries=%s", this.requestRetries)); 
        if (this.dryRun != null && this.dryRun) // dryRunArg
            commandList.add("--dry-run");
        if (this.backoffFactor != null && !this.backoffFactor.equals("")) // backoffFactorArg
            commandList.add(String.format("--backoff-factor=%s", this.backoffFactor));             

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

        private Secret clientSecretUI = Secret.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        public Secret getClientSecret(){
            return clientSecretUI;
        }

        public FormValidation doCheckServerURL(@QueryParameter String value) throws IOException{
            if(value.isEmpty()) {
                return FormValidation.error("Server URL cannot be empty.");
            }
            else if(!isValidURL(value)){
                return FormValidation.error("Invalid server_url.");
            }
            return FormValidation.ok();
        }
    
        public FormValidation doCheckTestConfigId(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Test Config Id cannot be empty.");
            }
            else if(!isValidUuid(value)){
                return FormValidation.error("Invalid test configuration id.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckClientId(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Client Id cannot be empty.");
            }
            return FormValidation.ok();
        }
       
        public FormValidation doCheckClientSecret(@QueryParameter String value) throws IOException {
            if(value.isEmpty()) {
                return FormValidation.error("Client Secret cannot be empty.");
            }
            else if(!isValidUuid(value)){
                return FormValidation.error("Invalid Client Secret.");
            }
            clientSecretUI = Secret.fromString(value);
            return FormValidation.ok();
        }
        
        public FormValidation doCheckPollInterval(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidNumeric(value)){
                return FormValidation.error("Invalid Poll Interval.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRequestTimeout(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidNumeric(value)){
                return FormValidation.error("Invalid Request Timeout.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckRequestRetries(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidNumeric(value)){
                return FormValidation.error("Invalid Request Retires.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckBackoffFactor(@QueryParameter String value) throws IOException {
            if(!value.isEmpty()&&!isValidDecimal(value)){
                return FormValidation.error("Invalid Backoff Factor.");
            }
            return FormValidation.ok();
        }

        private Boolean isValidURL(String value){
            try {
                new URL(value).toURI();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private Boolean isValidUuid(String value){
            Pattern p = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
            Boolean isMatch=p.matcher(value).matches();
            if(isMatch)
                return true;
            else
                return false;
        }

        private Boolean isValidNumeric(String value){
            Pattern p = Pattern.compile("^\\d+$");
            Boolean isMatch=p.matcher(value).matches();
            if(isMatch)
                return true;
            else
                return false;
        }

        private Boolean isValidDecimal(String value){
            Pattern p = Pattern.compile("^\\d+\\.?\\d*$");
            Boolean isMatch=p.matcher(value).matches();
            if(isMatch)
                return true;
            else
                return false;
        }

    }

}
