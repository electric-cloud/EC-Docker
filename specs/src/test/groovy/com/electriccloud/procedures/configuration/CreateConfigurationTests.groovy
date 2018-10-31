package com.electriccloud.procedures.configuration

import com.electriccloud.procedures.DockerTestBase
import com.electriccloud.test_data.ConfigurationData
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Issue
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import io.qameta.allure.TmsLinks
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*


@Feature("Configuration")
class CreateConfigurationTests extends DockerTestBase {

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerClient.deleteConfiguration(configName)
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        dockerClient.deleteConfiguration(configName)
        dockerClient.client.deleteProject(projectName)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        dockerClient.deleteConfiguration(configTls)
        dockerClient.deleteConfiguration(configSwarm)
        dockerClient.deleteConfiguration(configCommunity)
    }



    @Test(groups = "Positive")
    @Story("Create Configuration")
    @Description("Create Configuration for Docker-Swarm")
    void createConfigForDockerSwarm(){
        def jobId = dockerClient.createConfiguration(configName,
                endpointSwarm,
                userName,
                null,
                null,
                null,
                true, DEBUG).json.jobId
        String logs = dockerClient.client.getJobLogs(jobId)
        def jobStatus = dockerClient.client.getJobStatus(jobId).json
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Attaching credential to procedure Artifact2Image at step artifact2image")
    }



    @Test(groups = "Positive")
    @Story("Create Configuration")
    @Description(" Create Configuration for Docker-Community")
    void createConfigForDockerCommunity(){
        def jobId = dockerClient.createConfiguration(configName,
                endpointCommunity,
                userName,
                null,
                null,
                null,
                true, DEBUG).json.jobId
        def logs = dockerClient.client.getJobLogs(jobId)
        def jobStatus = dockerClient.client.getJobStatus(jobId).json
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Attaching credential to procedure Artifact2Image at step artifact2image")
    }




    @Test(groups = "Positive")
    @Story("Create Configuration")
    @Description("Create Configuration for Docker-Tls")
    void createConfigForDockerTls(){
        def job = ectoolApi.runDsl('docker', 'createConfigTls')
        def logs = dockerClient.client.getJobLogs(job)
        def jobStatus = dockerClient.client.getJobStatus(job).json
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Attaching credential to procedure Artifact2Image at step artifact2image")
    }




    @Test(groups = "Positive")
    @Story("Create Configuration")
    @Description("Create configuration without cluster test connection")
    void createConfigurationWithoutTestConnection(){
        def jobId = dockerClient.createConfiguration(configName, 'http://10.200.1.111:2376',
                userName,
                null,
                null,
                null,
                false, DEBUG).json.jobId
        def logs = dockerClient.client.getJobLogs(jobId)
        def jobStatus = dockerClient.client.getJobStatus(jobId).json
        def jobStep = dockerClient.client.getJobSteps(jobId).json.object.find { it.jobStep.stepName == 'testConnection' }
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("Attaching credential to procedure Artifact2Image at step artifact2image")
        assert jobStep.jobStep.combinedStatus.status == "skipped"

    }



    @Test(groups = "Negative", dataProvider = "logLevels", dataProviderClass = ConfigurationData.class)
    @Story("Create Configuration")
    @Description("Create Configuration for different log Levels ")
    void createConfigurationWithDifferentLogLevels(logLevel, message, desiredLog, missingLog){
        def jobId = dockerClient.createConfiguration(configName,
                endpointSwarm,
                userName,
                null,
                null,
                null,
                true, logLevel).json.jobId
        dockerClient.createEnvironment(configName)
        def resp = dockerClient.provisionEnvironment(projectName, environmentName, clusterName)
        def jobStatus = dockerClient.client.getJobStatus(jobId).json
        def jobSteps = dockerClient.client.getJobSteps(jobId).json.object
        def logs = dockerClient.client.getJobLogs(jobId)
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert jobSteps[2].jobStep.command.contains(message)
        assert logs.contains(desiredLog)
        assert !logs.contains(missingLog)
    }




    @Test(groups = "Negative")
    @Story("Invalid configuration")
    @Description("Unable to create configuration that already exist")
    void unnableToCreateExistingConfiguration(){
        try {
            dockerClient.createConfiguration(configName,
                    endpointSwarm,
                    userName,
                    null,
                    null,
                    null,
                    true, DEBUG)
            dockerClient.createConfiguration(configName,
                    endpointSwarm,
                    userName,
                    null,
                    null,
                    null,
                    true, DEBUG)
        } catch (e){
            def jobId = e.cause.message
            def jobStatus = dockerClient.client.getJobStatus(jobId).json
            def logs = dockerClient.client.getJobLogs(jobId)
            assert logs.contains("A configuration named '${configName}' already exists.")
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }



    @Test(groups = "Negative", dataProvider = "invalidConfigData", dataProviderClass = ConfigurationData.class)
    @Story("Invalid configuration")
    @Description("Unable to configure with invalid data")
    void unnableToConfigureWithInvalidData(configName, endpoint, caCert, cert, key, testConnection, logLevel, errorMessage){
        def jobStatus = null
        def logs = " "
        try {
            dockerClient.createConfiguration(configName,
                    endpoint,
                    userName,
                    caCert,
                    cert,
                    key,
                    testConnection, logLevel)
        } catch (e){
            def jobId = e.cause.message
            jobStatus = dockerClient.client.getJobStatus(jobId).json
            logs = dockerClient.client.getJobLogs(jobId)
        } finally {
            assert logs.contains(errorMessage), 'The Procedure passed with invalid credentials!!!'
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }






}