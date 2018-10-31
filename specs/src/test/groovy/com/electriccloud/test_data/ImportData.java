package com.electriccloud.test_data;

import com.electriccloud.procedures.DockerTestBase;
import org.testng.annotations.DataProvider;
import static com.electriccloud.procedures.DockerTestBase.*;

public class ImportData {

    @DataProvider(name = "importData")
    public Object[][] getImprtData(){
        return new Object[][]{
                {
                    serviceName, false, null, projectName, environmentName, "",
                        "Either specify all the parameters required to identify the Docker-backed ElectricFlow cluster"
                },
                {
                    serviceName, false, null, projectName, environmentName,
                        "my-cluster", "Cluster \'my-cluster\' does not exist in \'" + environmentName + "\' environment."
                },
                {
                    serviceName, false, null, projectName, "",clusterName,
                        "Either specify all the parameters required to identify the Docker-backed ElectricFlow cluster"
                },
                {
                    serviceName, false, null, projectName, environmentName, clusterName,
                        "Environment \'my-environment\' does not exist in project \'" + projectName + "\'"
                },
                {
                    serviceName, false, null, "Default", environmentName, clusterName,
                        "Environment \'" + environmentName + "\' does not exist in project \'Default\'"
                },
                {
                    "nginx-service-invalid", false, null, projectName, environmentName, clusterName,
                        "ERROR: Failed to read the Docker Compose file contents"
                },
                {
                    applicationName, true, applicationName, projectName, environmentName, "",
                        "Either specify all the parameters required to identify the Docker-backed ElectricFlow cluster"
                },
                {
                    applicationName, true, applicationName, projectName, environmentName, "my-cluster",
                        "Cluster \'my-cluster\' does not exist in \'" + environmentName + "\' environment."
                },
                {
                    applicationName, true, applicationName, projectName, "", clusterName,
                        "Either specify all the parameters required to identify the Docker-backed ElectricFlow cluster"
                },
                {
                    applicationName, true, applicationName, projectName, "my-environment", clusterName,
                        "Environment \'my-environment\' does not exist in project \'" + projectName + "\'"
                },
                {
                    applicationName, true, applicationName, "Default", environmentName, clusterName,
                        "Environment \'" + environmentName + "\' does not exist in project \'Default\'"
                },
                {
                    "nginx-service-invalid", true, applicationName, projectName, environmentName, clusterName,
                        "ERROR: Failed to read the Docker Compose file contents"
                },
                {
                    applicationName, true, "", projectName, environmentName, clusterName,
                        "Application name is required for creating application-scoped microservices"
                }
        };
    }






}
