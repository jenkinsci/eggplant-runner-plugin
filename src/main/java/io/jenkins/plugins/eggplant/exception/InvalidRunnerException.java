package io.jenkins.plugins.eggplant.exception;

import hudson.AbortException;

public class InvalidRunnerException extends AbortException {
  public InvalidRunnerException(String message) {
    super(message);
  }
}
