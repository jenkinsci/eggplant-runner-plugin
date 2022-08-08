package io.jenkins.plugins.eggplant.exception;

import hudson.AbortException;

public class CLIExitException extends AbortException {
  public CLIExitException(int exitCode) {
    super("Eggplant Runner CLI exited with code '" + exitCode + "'.");
  }
}
