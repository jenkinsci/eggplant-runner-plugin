package io.jenkins.plugins.eggplant.exception;

import hudson.AbortException;

public class BuilderException extends AbortException {
  public BuilderException(String message) {
    super(message);
  }
}
