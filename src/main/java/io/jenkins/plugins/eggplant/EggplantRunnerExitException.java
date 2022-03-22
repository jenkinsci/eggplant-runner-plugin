package io.jenkins.plugins.eggplant;

import hudson.AbortException;

public class EggplantRunnerExitException extends AbortException {
  EggplantRunnerExitException(int exitCode) {
    super("Eggplant Runner CLI exited with code '" + exitCode + "'.");
  }
}
