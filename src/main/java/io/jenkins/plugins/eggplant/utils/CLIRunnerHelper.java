package io.jenkins.plugins.eggplant.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.net.Proxy;
import hudson.FilePath;
import io.jenkins.plugins.eggplant.common.OperatingSystem;
import io.jenkins.plugins.eggplant.exception.BuilderException;
import io.jenkins.plugins.eggplant.exception.InvalidRunnerException;
import jenkins.model.Jenkins;

public class CLIRunnerHelper{

  private final static String CLI_VERSION = "7.5.0-7";  
  private final static Map<OperatingSystem, String> CLI_FILENAME = Stream.of(
      new AbstractMap.SimpleEntry<>(OperatingSystem.LINUX, "eggplant-runner-Linux-${cliVersion}"),
      new AbstractMap.SimpleEntry<>(OperatingSystem.MACOS, "eggplant-runner-MacOS-${cliVersion}"), 
      new AbstractMap.SimpleEntry<>(OperatingSystem.WINDOWS, "eggplant-runner-Windows-${cliVersion}.exe")
  ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  private final static String CLI_DOWNLOAD_URL = "https://assets.eggplantsoftware.com/EggplantRunner/${cliFilename}";
  private final static String CLI_ENG_DOWNLOAD_URL = "https://gitlab.com/api/v4/projects/22402994/packages/generic/${cliVersion}/0.0.0/${cliFilename}";

  private PrintStream logger;
  private FilePath workspace;
  private String cliFilename;
  private FilePath cliFilePath;
  
  public CLIRunnerHelper(FilePath workspace, OperatingSystem os, PrintStream logger){
    this.workspace = workspace;
    this.cliFilename = CLI_FILENAME.get(os).replace("${cliVersion}", CLI_VERSION.toLowerCase()); // CLI filename must be lowercase
    this.cliFilePath = workspace.child("downloads").child(cliFilename);
    this.logger = logger;
  }

  public FilePath getFilePath(){
    return cliFilePath;
  }

  public String getFilename(){
    return cliFilename;
  }

  public String getPublicDownloadLink(){
    return CLI_DOWNLOAD_URL.replace("${cliFilename}", cliFilename); 
  }

  public void copyRunnerFrom(String path) throws IOException, InterruptedException
  {
    logger.println(">> Checking runner...");
    FilePath filePath = CLIValidation(path);
    logger.println("Fetching runner from " + path);
    cliFilePath.copyFrom(filePath);
    logger.println("Fetch complete.");
  }

  public FilePath CLIValidation(String path) throws IOException
  {
    File file = new File(path);    
    if(file.canRead() == false)
    {
      /* if the file does not exist, read access would be denied because the Java virtual machine has 
      insufficient privileges, or access cannot be determined */
      throw new InvalidRunnerException("No such file or permission denied. Eggplant Runner Path: " + path);
    }

    if(file.isDirectory() == true)
    {
      throw new InvalidRunnerException("Path provided must not be a directory. Eggplant Runner Path: " + path);
    }

    FilePath filePath = new FilePath(file);

    String filePathMinor = "";
    String cliFilenameMinor = "";
    String fileName = filePath.getName();

    if (cliFilename.length() == 29) {
      cliFilenameMinor = cliFilename.substring(0, cliFilename.length() - 4);
      filePathMinor = fileName.substring(0, fileName.length() - 4);
    } else if (cliFilename.length() == 35) {
      cliFilenameMinor = cliFilename.substring(0, cliFilename.length() - 8);
      filePathMinor = fileName.substring(0, fileName.length() - 8);
    } else {
      cliFilenameMinor = "not correct file format";
      filePathMinor = "not match";
    }

    if(!filePathMinor.equals(cliFilenameMinor))
    {
      throw new InvalidRunnerException("File found is invalid. Required: " + cliFilename + ". Please download from " +  getPublicDownloadLink());
    }
    return filePath;
  }

  public void downloadRunner(String gitlabAccessToken) throws IOException, InterruptedException{
    String cliDownloadUrl = getPublicDownloadLink();   
    Map<String,String> properties = new HashMap<>();

    logger.println(">> Downloading runner...");

    if(gitlabAccessToken == null){
      FilePath defaultCache = workspace.child("downloads").child(cliFilename);
      if(defaultCache.exists())
      {
        logger.println("Runner found in default directory, skipping download.");
        return;
      }
    }
    else{      
      cliDownloadUrl = CLI_ENG_DOWNLOAD_URL.replace("${cliVersion}", CLI_VERSION).replace("${cliFilename}", cliFilename);
      properties.put("PRIVATE-TOKEN", gitlabAccessToken);
      cliFilePath = workspace.child("downloads").child("eng").child(cliFilename);
    }
    downloadFromUrl(cliDownloadUrl, properties);
  }

  public Proxy setProxy(String ip, int port, String username, String password) {    
    Authenticator.setDefault(new Authenticator() {
      @Override
      public PasswordAuthentication getPasswordAuthentication() {
          if (getRequestorType() == RequestorType.PROXY) {
              if (getRequestingHost().equalsIgnoreCase(ip)) {
                  if (port == getRequestingPort()) {
                      return new PasswordAuthentication(username, password.toCharArray());  
                  }
              }
          }
          return null;
      }  
    });
    System.setProperty("http.proxyHost", ip);
    System.setProperty("http.proxyPort", String.valueOf(port));
    System.setProperty("http.proxyUser", username);
    System.setProperty("http.proxyPassword", password);
    System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
  }
  
  private FilePath downloadFromUrl(String url, Map<String, String> properties) throws BuilderException {

    try{

      logger.println("GET " + url);      
      HttpURLConnection connection = null;

      if (Jenkins.get().proxy != null)
      {
        String proxyHostname = Jenkins.get().proxy.name;
        int port = Jenkins.get().proxy.port;
        String proxyUsername = Jenkins.get().proxy.getUserName();
        String proxyPassword = Jenkins.get().proxy.getSecretPassword().getPlainText();
        Proxy proxy = this.setProxy(proxyHostname, port, proxyUsername, proxyPassword);
        logger.println("Connected through proxy server."); 
        connection = (HttpURLConnection) new URL(url).openConnection(proxy);
      }
      else
      {
        connection = (HttpURLConnection) new URL(url).openConnection();
      }

      for(Entry<String, String> entry: properties.entrySet()){
        connection.addRequestProperty (entry.getKey(), entry.getValue());
      }

      connection.setDoOutput(true);
      connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
      InputStream in = connection.getInputStream();
      cliFilePath.copyFrom(in);
      logger.println("Download successfully.");

    }catch(Exception e){
      throw new BuilderException("Download failed. Unable to download from url: "+ url +". Error details:" + e.toString());
    }

    return cliFilePath;
  }

}
