package com.electriccloud.client.api

import io.qameta.allure.Step
import org.apache.log4j.Logger
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder

class JfrogArtifactoryApi {

    Artifactory artifactory
    public static Logger log = Logger.getLogger("appLogger")


    JfrogArtifactoryApi(artifactoryUrl, username, password){
        this.artifactory = ArtifactoryClientBuilder.create()
                .setUrl(artifactoryUrl)
                .setUsername(username)
                .setPassword(password)
                .build()
    }

    @Step
    void uploadArtifact(File artifact, repoType, repoName, orgPath, artifactName, version, extension){
        def basePath
        if (repoType == "Maven"){
            basePath = "$orgPath/${artifactName}/${version}/${artifactName}-${version}.${extension}"
        } else {
            basePath = "$orgPath/${artifactName}/${artifactName}.${version}.${extension}"
        }
        log.info("Uploading artifact: " + basePath)
        artifactory.repository(repoName)
                .upload(basePath, artifact)
                .doUpload()
        log.info("Artifact: " + "$basePath is successfully uploaded!")

    }



}
