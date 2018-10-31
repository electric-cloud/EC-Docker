package com.electriccloud.procedures.import_ms

import com.electriccloud.helpers.enums.LogLevels
import com.electriccloud.procedures.DockerTestBase
import com.electriccloud.test_data.ImportData
import io.qameta.allure.Description
import io.qameta.allure.Feature
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import io.qameta.allure.TmsLinks
import org.testng.annotations.AfterClass
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*


@Feature('Import')
class ImportTests extends DockerTestBase {


    @BeforeClass(alwaysRun = true)
    void setUpTests(){
        dockerClient.deleteConfiguration(configName)
        dockerClient.createConfiguration(configName, endpointSwarm, userName, null, null, null, true, DEBUG)
        dockerClient.createEnvironment(configName)

    }

    @AfterClass(alwaysRun = true)
    void tearDownTests(){
        dockerClient.client.deleteProject(projectName)
    }

    @BeforeMethod(alwaysRun = true)
    void setUpTest(){
        dockerClient.client.deleteApplication(projectName, applicationName)
        dockerClient.client.deleteService(projectName, serviceName)
    }

    @AfterMethod(alwaysRun = true)
    void tearDownTest(){
        dockerClient.client.deleteApplication(projectName, applicationName)
        dockerClient.client.deleteService(projectName, serviceName)
    }


    @Test(groups = "Positive")
    @TmsLink("278845")
    @Story('Import Microservice')
    @Description("Import Project Level Microservice")
    void importProjectLevelMicroservice(){
        dockerClient.importService(serviceName,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null)
        def services = dockerClient.client.getServices(projectName).json.service
        def service = dockerClient.client.getService(projectName, serviceName).json.service
        def container = dockerClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable[1].environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable[1].value == "80"
    }



    @Test(groups = "Positive")
    @TmsLink("324697")
    @Story('Import Microservice')
    @Description("Import Application Level Microservice")
    void importApplicationLevelMicroservice(){
        dockerClient.importService(applicationName,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                true, applicationName)
        def apps = dockerClient.client.getApplications(projectName).json.application
        def app = dockerClient.client.getApplication(projectName, applicationName).json.application
        def container = dockerClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
        assert apps.size() == 1
        assert app.applicationName == applicationName
        assert app.containerCount == "1"
        assert app.projectName == projectName
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable[1].environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable[1].value == "80"
    }



    @Test
    @TmsLink("278856")
    @Story('Import Microservice')
    @Description("Import Existing Project Level Microservice")
    void importExistingProjectLevelMicroservice(){
        dockerClient.importService(serviceName,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null)
        def jobId = dockerClient.importService(serviceName,
                projectName,
                environmentProjectName,
                environmentName,
                clusterName,
                false, null).json.jobId
        def services = dockerClient.client.getServices(projectName).json.service
        def service = dockerClient.client.getService(projectName, serviceName).json.service
        def container = dockerClient.client.getServiceContainer(projectName, serviceName, containerName).json.container
        def jobLogs = dockerClient.client.getJobLogs(jobId)
        def jobStatus = dockerClient.client.getJobStatus(jobId).json
        assert services.size() == 1
        assert service.serviceName == serviceName
        assert service.containerCount == '1'
        assert service.environmentMapCount == '1'
        assert service.defaultCapacity == '2'
        assert service.projectName == projectName
        assert service.volume == "[{\"name\":\"html-content\",\"hostPath\":\"/var/html\"}]"
        assert container.containerName == containerName
        assert container.imageName == "nginx"
        assert container.imageVersion == "stable"
        assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
        assert container.environmentVariable[1].environmentVariableName == "NGINX_PORT"
        assert container.environmentVariable[1].value == "80"
        assert jobStatus.status == "completed"
        assert jobStatus.outcome == "warning"
        assert jobLogs.contains("Service ${serviceName} already exists, skipping")
    }


    @Test
    @TmsLink("324698")
    @Story('Import Microservice')
    @Description("Import Existing Application Level Microservice ")
    void importExistingApplicationLevelMicroservice(){
        try {
            dockerClient.importService(serviceName,
                    projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    true, applicationName)
            dockerClient.importService(serviceName,
                    projectName,
                    environmentProjectName,
                    environmentName,
                    clusterName,
                    true, applicationName).json.jobId
        } catch (e){
            def jobId = e.cause.message
            def errorLog = dockerClient.client.getJobLogs(jobId)
            def jobStatus = dockerClient.client.getJobStatus(jobId).json
            def apps = dockerClient.client.getApplications(projectName).json.application
            def app = dockerClient.client.getApplication(projectName, applicationName).json.application
            def container = dockerClient.client.getApplicationContainer(projectName, applicationName, serviceName, containerName).json.container
            assert apps.size() == 1
            assert app.applicationName == applicationName
            assert app.containerCount == "1"
            assert app.projectName == projectName
            assert container.containerName == containerName
            assert container.imageName == "nginx"
            assert container.imageVersion == "stable"
            assert container.volumeMount == "[{\"name\":\"html-content\",\"mountPath\":\"/usr/share/nginx/html\"}]"
            assert container.environmentVariable[1].environmentVariableName == "NGINX_PORT"
            assert container.environmentVariable[1].value == "80"
            assert jobStatus.status == "completed"
            assert jobStatus.outcome == "error"
            assert errorLog.contains("Application \'${applicationName}\' already exists in the project \'${projectName}\'")
        }


    }


    @Test(dataProvider = 'importData', dataProviderClass = ImportData.class)
    @TmsLinks(value = [
            @TmsLink("278854"),
            @TmsLink("278858"),
            @TmsLink("278859"),
            @TmsLink("278860")
    ])
    @Story("Import Microservice with invalid data")
    @Description("Import Microservice with invalid data")
    void invalidServiceImport(yamlFile, isApp, appName, project, envName, clusterName, errorMessage){
        try {
            dockerClient.importService(yamlFile, project, project, envName, clusterName, isApp, appName)
        } catch (e){
            def jobId = e.cause.message
            String errorLog = dockerClient.client.getJobLogs(jobId)
            def jobStatus = dockerClient.client.getJobStatus(jobId).json
            assert errorLog.contains(errorMessage)
            assert jobStatus.outcome == "error"
            assert jobStatus.status == "completed"
        }
    }



}
