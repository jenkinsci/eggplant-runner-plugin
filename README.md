# Eggplant Runner Jenkins Plugin (ALPHA VERSION)

# THIS IS STILL IN TEST, WAIT FOR OFFICIAL RELEASE

## Introduction

Eggplant Runner is a Jenkins plugin that runs DAI Test Config .

## Getting started

The plugin can be used by executing it as follows in your Jenkinsfile:

```yaml
pipeline {
    agent any

    environment {
        DAI_CLIENT_SECRET = client-secret
    }

    stages {
        stage('Eggplant Runner') {
            steps {
                serverURL: 'https://edge.dai.webperfdev.com/', testConfigId: '307fee3e-9d6b-43e6-b31e-f1d379f27cdf', eggplantRunner clientSecret: 'secret'
            }
        }
    }
}
```

It can also be configured as a Build Step using the Jenkins GUI.

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

## MANTAINER

For development, we need to installe the below.

1. You can download and install Java 11 from the Eclipse Temurin website (https://adoptium.net/).
2. Download Maven from Apache Maven website (https://maven.apache.org/). Make sure to download one of the binary archives (with bin in their name).
3. To verify that Maven is installed, run the following command: mvn -version
4. You an use launch.json to run 'Debug (Attach)' to launch an local Jenkins instance for development.
