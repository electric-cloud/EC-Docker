package com.electriccloud.procedures.configuration

import com.electriccloud.procedures.DockerTestBase
import io.qameta.allure.TmsLink
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test


class CheckClusterTests extends DockerTestBase {

    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerClient.deleteConfiguration(configSwarm)
        dockerClient.createConfiguration(configSwarm, endpointCommunity, userName)
    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        dockerClient.client.deleteProject(projectName)
    }



    @Test(groups = "Positive")
    void checkDockerCluster(){
        def jobId = dockerClient.checkCluster(configSwarm).json.jobId
        def logs = dockerClient.client.getJobLogs(jobId)
        def jobStatus = dockerClient.client.getJobStatus(jobId).json
        assert jobStatus.outcome == "success"
        assert jobStatus.status == "completed"
        assert logs.contains("DockerClient status: de.gesellix.docker.engine.EngineResponseStatus(text:OK, code:200, success:true)")
    }


    @Test(groups = "Negative")
    void checkDockerClusterWithInvalidConfiguration(){
        try {
            dockerClient.checkCluster("test")
        } catch (e){
            def logs = dockerClient.client.getJobLogs(e.cause.message)
            def jobStatus = dockerClient.client.getJobStatus(e.cause.message).json
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
            assert logs.contains("ERROR: Configuration test does not exist!")
        }

    }




}
