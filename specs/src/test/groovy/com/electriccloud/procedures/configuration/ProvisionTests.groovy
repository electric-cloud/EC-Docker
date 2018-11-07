package com.electriccloud.procedures.configuration

import com.electriccloud.procedures.DockerTestBase
import com.electriccloud.test_data.ConfigurationData
import io.qameta.allure.Description
import io.qameta.allure.Story
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*

class ProvisionTests extends DockerTestBase {


    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerClient.deleteConfiguration(configName)
        dockerClient.createConfiguration(configName, endpointSwarm, userName, null, null, null, true, DEBUG)
        dockerClient.createEnvironment(configName)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        dockerClient.deleteConfiguration(configName)
        dockerClient.client.deleteProject(environmentProjectName)
    }



    @Test(groups = "Positive")
    @Story("Provisioning of docker environment")
    @Description("Provision Docker cluster")
    void provisionCluster(){
        def jobId = dockerClient.provisionEnvironment(projectName, environmentName, clusterName).json.jobId
        def jobStatus = dockerClient.client.getJobStatus(jobId)
        def jobLogs = dockerClient.client.getJobLogs(jobId)
        assert jobStatus.json.outcome == "success"
        assert jobStatus.json.status == "completed"
        assert jobLogs.contains("INFO de.gesellix.docker.client.DockerClientImpl - using docker at \'${endpointSwarm}\'")

    }


    @Test(groups = "Negative", dataProvider = 'invalidProvisionData', dataProviderClass = ConfigurationData.class)
    @Story('Provisioning of docker environment with invalid data')
    @Description("Provision Docker cluster with invalid data")
    void invalidClusterProvisioning(project, environment, cluster, message){
        try {
            dockerClient.provisionEnvironment(project, environment, cluster).json
        } catch (e){
            assert e.cause.message.contains(message)
      }
    }





}
