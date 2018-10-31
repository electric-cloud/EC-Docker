package com.electriccloud.test_data;

import com.electriccloud.helpers.enums.LogLevels;
import org.testng.annotations.DataProvider;

import static com.electriccloud.helpers.enums.LogLevels.LogLevel.*;
import static com.electriccloud.procedures.DockerTestBase.*;

public class ConfigurationData {

    @DataProvider(name = "logLevels")
    public Object[][] getLogLevels(){
        return new Object[][]{
                {DEBUG,"logger DEBUG", "[INFO]", "[ERROR]"},
                {INFO, "logger INFO", "[INFO]", "[DEBUG]"},
                {WARNING, "logger WARNING", "[INFO]", "[DEBUG]"},
                {ERROR, "logger ERROR", "[INFO]", "[DEBUG]"},
        };
    }


    @DataProvider(name = "invalidConfigData")
    public Object[][] getInvalidData(){
        return new Object[][]{
                {
                    " ", endpointSwarm, null, null, null, true, DEBUG,
                        "configuration credential: \'credentialName\' is required and must be between 1 and 255 characters"
                },
                {
                    configName, " ", null, null, null, true, DEBUG,
                        "ERROR: Connection check for Docker endpoint \' \' failed"
                },
                {
                    configName, "http://10.200.1.321:2376 ", null, null, null, true, DEBUG,
                        "ERROR: Connection check for Docker endpoint 'http://10.200.1.321:2376 ' failed"
                },
                {
                    configName, endpointTls , null, null, null, true, DEBUG,
                        "400 Bad Request"
                },
                {
                    configName, endpointTls , "test-test", null, null, true, DEBUG,
                        "400 Bad Request"
                },
                {
                    configName, endpointTls , null, "test-test", null, true, DEBUG,
                        "400 Bad Request"
                },
                {
                    configName, endpointTls , null, null, "test-test", true, DEBUG,
                        "ERROR: Connection check for Docker endpoint '" + endpointTls +"' failed: java.security.GeneralSecurityException: Cannot generate private key from file"
                }
        };
    }


    @DataProvider(name = "invalidProvisionData")
    public Object[][] getProvisionData(){
        return new Object[][]{
                {
                        "test", environmentName, clusterName, "NoSuchProject: Project 'test' does not exist"
                },
                {
                        "Default", environmentName, clusterName, "NoSuchEnvironment: Environment '" + environmentName +"' does not exist in project 'Default'"
                },
                {
                        projectName, "test", clusterName, "NoSuchEnvironment: Environment 'test' does not exist in project '" + projectName + "'"
                },
                {
                        projectName, environmentName, "test-cluster", "NoSuchCluster: Cluster 'test-cluster' does not exist in environment '" + environmentName + "'"
                }
        };
    }





}
