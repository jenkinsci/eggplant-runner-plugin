package io.jenkins.plugins.eggplant.common;

public enum LogLevel {
    INFO("INFO"),
    DEBUG("DEBUG"),
    WARNING("WARNING"),
    ERROR("ERROR");
  
    private final String logLevel;
  
    LogLevel(String logLevel) {
      this.logLevel = logLevel;
    }
  
    public String getLogLevel() {
      return logLevel;
    }
  }
