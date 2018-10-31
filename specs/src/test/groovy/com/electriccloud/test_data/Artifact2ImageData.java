package com.electriccloud.test_data;

import com.electriccloud.helpers.objects.Artifactory;
import org.testng.annotations.DataProvider;

import static com.electriccloud.helpers.enums.RepoTypes.RepoType.GENERIC;
import static com.electriccloud.helpers.enums.RepoTypes.RepoType.MAVEN;
import static com.electriccloud.helpers.enums.RepoTypes.RepoType.NUGET;
import static com.electriccloud.procedures.DockerTestBase.*;

public class Artifact2ImageData {


    @DataProvider(name = "artifactoryTypeData")
    public Object[][] getArtifactoryTypeData(){
        return new Object[][]{
                {
                        "hello-world-war-1.0.0.war",
                        new Artifactory(artifactoryConfig, MAVEN, "libs-release-local", "com/mycompany", "hello-world-war", "1.0.0", "war"),
                        dockerHubId + "/" + jettyRepo,
                        jettyRepo,
                        "jetty",
                        "8080",
                        false
                },
                {
                        "com.mycompany.AspNetSample.1.0.0.zip",
                        new Artifactory(artifactoryConfig, NUGET, "nuget-local", "com/mycompany", "AspNetSample", "1.0.0", "nupkg"),
                        dockerHubId + "/" + netRepo,
                        netRepo,
                        null,
                        "80",
                        false
                },
                {
                        "com.mycompany.AspNetSample.1.0.0.zip",
                        new Artifactory(artifactoryConfig, GENERIC, "generic-local", "com/mycompany", "AspNetSample", "1.0.0", "zip"),
                        dockerHubId + "/" + netRepo,
                        netRepo,
                        null,
                        "80",
                        false
                }
        };

    }



    @DataProvider(name = "templateTypeData")
    public Object[][] getArtifactsTemplateTypeData(){
        return new Object[][]{
                {
                        jettyArtifact,
                        "1.0.0",
                        artifactsDir,
                        "hello-world-war-1.0.0.war",
                        dockerHubId + "/" + jettyRepo,
                        jettyRepo,
                        new Artifactory(null, MAVEN, null, null, null, null, null),
                        "jetty",
                        "8080",
                        null,
                        null,
                        false
                },
                {
                        jarArtifact,
                        "1.0.0",
                        artifactsDir,
                        "npweb-0.0.1-SNAPSHOT.jar",
                        dockerHubId + "/" + jarRepo,
                        jarRepo,
                        new Artifactory(null, MAVEN, null, null, null, null, null),
                        "openjdk:8-jre-alpine",
                        "8080",
                        null,
                        "\"/usr/bin/java\", \"-jar\", \"/npweb-app.jar\"",
                        false
                },
                {
                        netArtifact,
                        "1.0.0",
                        artifactsDir,
                        "com.mycompany.AspNetSample.1.0.0.zip",
                        dockerHubId + "/" + netRepo,
                        netRepo,
                        new Artifactory(null, MAVEN, null, null, null, null, null),
                        null,
                        "80",
                        null,
                        null,
                        false
                }
        };
    }




}
