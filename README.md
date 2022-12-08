<img src="https://www.eggplantsoftware.com/hubfs/Branding/Keysight-Eggplant-Logo_RGB_full-color.svg" width="300px"/>

# Eggplant DAI Plugin for Jenkins
 
## Introduction

The [Eggplant DAI](https://www.eggplantsoftware.com/digital-automation-intelligence) Plugin for Jenkins launches DAI tests from within a Jenkins pipeline.  You can use it to continuously test your application using Eggplant's [model-based approach to testing](https://docs.eggplantsoftware.com/docs/dai-using-eggplant-dai/).  For more information about Eggplant, visit https://www.eggplantsoftware.com.

## Install Eggplant DAI Plugin for Jenkins

**Step 1:** Login to your Jenkins

Go to your Jenkins home page and login into your Jenkins account
![image](https://user-images.githubusercontent.com/101400930/165949547-26ab3829-466f-478f-ad36-04427d2c7da3.png)

**Step 2**: Manage Jenkins

- Go to Manage Jenkins > Manage Plugins
![image](https://user-images.githubusercontent.com/101400930/165950020-f80e906b-1c52-4f38-8902-a1cb61019a44.png)

- Go to **Available** and enter **Eggplant Runner** in the search feature
- Check on the **Eggplant Runner** and click on **Install without restart**
![image](https://user-images.githubusercontent.com/101400930/165950313-e11ce24c-d424-40a5-83d4-d1d752cbdb35.png)

## How to use Eggplant DAI Plugin for Jenkins

### Freestyle project

**Step 1** : Create new project -> **Freestyle project** -> Click **OK**

![image](https://user-images.githubusercontent.com/101400930/166857604-d512f1b9-95a8-48e3-b884-023690fb68ad.png)

**Step 2**: Add **Eggplant Runner** as build step

![image](https://user-images.githubusercontent.com/101400930/166877164-b314f345-1295-401c-a63b-4da989b80579.png)

**Step 3**: Fill in **Eggplant Runner** parameter's value -> Click **Save**

![image](https://user-images.githubusercontent.com/103989779/199642774-f776a734-e0de-4991-b0e5-2e2779483dbe.png)


## Inputs

### `serverURL`
**Required** The URL of the Eggplant DAI server, `http(s)://dai_server_hostname:portnumber`.

### `testConfigID`
**Required** The ID of the Eggplant DAI test configuration that you want to run, e.g. `09c48b7d-fc5b-481d-af80-fcffad5d9587`.
Test configuration ID can be obtain by go to test config > look for a particular test config > test config id can be obtain from url.
![image](https://user-images.githubusercontent.com/103989779/199636740-57d4bfd2-3c94-449c-b2d5-597d69d2f03e.png)


### `clientSecret`
**Required** The client secret to use to authenticate with the Eggplant DAI server, e.g. `e9c15662-8c1b-472e-930d-aa0b11726093`.<br />
             Alternatively, you could set a repo secret in `Repo Settings > Secrets > Actions` and refer to it like below:<br />
             `clientSecret: "${{ secrets.DAI_CLIENT_SECRET }}"`.

The **DAI Client Secret** can be obtain by go to http://dai_server_hostname:portnumber/auth > client > search for client:dai:agent:integration > credential
![image](https://user-images.githubusercontent.com/103989779/199636556-700ac626-87ad-4373-ab97-b78773f25176.png)

             
### `clientID`
**Required** The client ID to use to authenticate with the Eggplant DAI server.<br />
**Default:** `client:dai:agent:integration`

### `requestTimeout`
**Optional** The timeout in seconds for each HTTP request to the Eggplant DAI server<br />
**Default:** `30`

### `requestRetries`
**Optional** The number of times to attempt each HTTP request to the Eggplant DAI server<br />
**Default:** `5`

### `backoffFactor`
**Optional** The exponential backoff factor between each HTTP request<br />
**Default:** `0.5`

### `pollInterval`
**Optional** The number of seconds to wait between each call to the Eggplant DAI server<br />
**Default:** `5`

### `testEnvironmentTimeout`
**Optional** The timeout in seconds for checking test environment readiness<br />
**Default:** `15`

### `logLevel`
**Optional** The logging level<br />
**Default:** `INFO`

### `CACertPath`
**Optional** The path to an alternative Certificate Authority pem file<br />

### `testResultPath`
**Optional** Path to a file where the test results will be stored in junit xml format<br />
**Example** `C:\results\result.xml`

### `eggplantRunnerPath`
**Optional** The path to eggplant runner CLI executable<br />

## Output

**Build** the project to run the step
![image](https://user-images.githubusercontent.com/101400930/166877993-8fa5b585-205e-44c6-8c33-b03ded69cd83.png)

Execution details are shown in **Console Output**
![image](https://user-images.githubusercontent.com/101400930/169545643-8d83d7c5-be94-46b6-9a39-605353aafeb7.png)


## Advanced Usage

**Pipeline project**

**Step 1**: Create **Pipeline** project

**Step 2**: Copy and paste below scrips into **Pipiline script** section
```yaml
pipeline {
    agent any

    environment {
        DAI_CLIENT_SECRET = credentials('eggplant-runner-client-secret')
    }

    stages {
        stage('Eggplant Runner') {
            steps {
                eggplantRunner serverURL: 'Your DAI server URL', testConfigId: 'Your test configuration that want to execute'
            }
        }
    }
}
```

>:warning: **Usage of backslashes in script**: Remember to escape backslashes by replace all instances of `\` with `\\` inside a string.</br>For example, rather than:</br>`CACertPath: "C:\certs\rootCA.cer"`</br>you would use:</br>`CACertPath: "C:\\certs\\rootCA.cer"`

![image](https://user-images.githubusercontent.com/101400930/168762767-80706d26-e892-4ee1-bd7b-9490ba04ff80.png)

**Step 3**: Setup **Public Credential** for **Client Secret** -> Click **Ok**
![image](https://user-images.githubusercontent.com/101400930/166881666-1d6512cb-86b2-4027-a574-8314f19d707d.png)

**Step 4**: **Build** the pipeline project
Output are shown in **Console Output**
![image](https://user-images.githubusercontent.com/101400930/169546010-1fce3d53-daa2-42e4-8945-0f6f49870d5d.png)

## Release for DAI 
<table>
  <thead>
    <tr>
      <th width="300px">DAI Version</th>
      <th width="500px">Release</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>6.4.0-3</td>
      <td><a href="https://plugins.jenkins.io/eggplant-runner/">latest</a></td>
  </tr>
  <tr>
      <td>6.3.0-3</td>
      <td><a href="https://updates.jenkins.io/download/plugins/eggplant-runner/0.0.1.191.v72dea_07931b_6/eggplant-runner.hpi"> 0.0.1.191.v72dea_07931b_6</a> | <a href="https://updates.jenkins.io/download/plugins/eggplant-runner/0.0.1.189.v1e3397db_cee8/eggplant-runner.hpi">0.0.1.189.v1e3397db_cee8</a></td>
  </tr>
  <tr>
      <td>6.2.1-2</td>
      <td><a href="https://updates.jenkins.io/download/plugins/eggplant-runner/0.0.1.185.v9617008ee458/eggplant-runner.hpi">0.0.1.185.v9617008ee458</a> | <a href="https://updates.jenkins.io/download/plugins/eggplant-runner/0.0.1.159.v8ed1d9f67f00/eggplant-runner.hpi">0.0.1.159.v8ed1d9f67f00</a></td>
  </tr>
  <tr>
      <td>6.1.2-1</td>
      <td><a href="https://updates.jenkins.io/download/plugins/eggplant-runner/0.0.1.108.v32f1564b_19d0/eggplant-runner.hpi">0.0.1.108.v32f1564b_19d0</a></td>
  </tr>
  </tbody>
</table>

## License

This plug-in is licensed under the terms of the [MIT license](LICENSE.md)

## Releases Note

30th May 2022

- Fix Jenkins Plugins "Eggplant Runner" Client Secret textbox behaviour

## Contributing

You need to install the following dependencies if you want to contribute to the Eggplant DAI Runner for Jenkins:

1. You can download and install Java 11 from the [Eclipse Temurin website](https://adoptium.net/).
2. Download Maven from the [Apache Maven website](https://maven.apache.org/). Make sure to download one of the binary archives (with bin in their name).
3. To verify that Maven is installed, run the following command: `mvn -version`
4. You can use `launch.json` to run 'Debug (Attach)' to launch an local Jenkins instance for development. 
