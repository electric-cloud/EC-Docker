package com.electriccloud.helpers.json


import com.electriccloud.helpers.objects.Artifactory
import groovy.json.JsonBuilder

class JsonHelper {

    private json = new JsonBuilder()



    /**
     * Service Mapping
     */

    def mapingJson = { project, service ->
        json.names {
            projectName "${project}"
            serviceName "${service}"
        }
        json
    }

    /**
     * Deploy Json
     */


    def deployJson = { projectName, environmentName, environmentProjectName, serviceName ->
        json.names {
            project "${projectName}"
            environment "${environmentName}"
            envProject "${environmentProjectName}"
            service "${serviceName}"
        }
        json
    }


    def deployAppJson = { projectName, applicationName, tierMappingName ->
        json.names {
            project "${projectName}"
            appName "${applicationName}"
            tierMapName "${tierMappingName}"
        }
        json
    }

    def confJson = { configurationName ->
        json.names {
            configName "${configurationName}"
        }
        json
    }

    def envJson = { projectName, configurationName ->
        json.names {
            projName "${projectName}"
            configName "${configurationName}"
        }
        json
    }

    /**
     * Import
     */

    def importJson = { yamlText, project, envProject, envName, cluster, importApp = false, appName ->
        def appScoped
        if (importApp){
            appScoped = "1"
        } else {
            appScoped = null
        }
        json.names {
            templateYaml "${yamlText}"
            projectName project
            applicationScoped appScoped
            applicationName appName
            envProjectName envProject
            environmentName envName
            clusterName cluster
        }
        json
    }

    /**
     * Provisoning
     */

    def provisionJson = { project, environment, clusterName ->
        json.names {
            projectName project
            environmentName environment
            cluster clusterName
        }
        json
    }





    def configJson = { configName, endpoint, userName, caCert, cert, key, testConnect, logLev  ->
        json.names {
            config configName
            dockerEndpoint endpoint
            username userName
            caPem caCert
            certPem cert
            keyPem key
            testConnection testConnect
            logLevel logLev
        }
        json
    }


    def serviceJson = { replicaNum, volumes = [source: null, target: null ] ->
        json.names {
            replicas replicaNum
            sourceVolume volumes.source
            targetVolume volumes.target
        }
        json
    }


    def artifact2ImageJson = { confName,
                               artifactsName,
                               artifactsVersion,
                               artifactsLocation,
                               credential,
                               Artifactory artifactory,
                               image,
                               registry,
                               bImage,
                               exposedPorts, command, environments, removeAfter ->
        json.names {
            credentialName credential.credName
            configName confName
            imageName image
            artifactName artifactsName
            artifactLocation artifactsLocation
            artifactoryArtifactName artifactory.artifactName
            artifactoryConfig artifactory.config
            artifactoryArtifactExtension artifactory.artifactExtension
            artifactoryOrgPath artifactory.orgPath
            artifactoryRepoKey artifactory.repoKey
            artifactoryRepoType artifactory.repoType
            artifactoryArtifactVersion artifactory.artifactVersion
            exposePorts exposedPorts
            env environments
            cmd command
            baseImage bImage
            registryUrl registry
            artifactVersion artifactsVersion
            removeAfterPush removeAfter
            userName credential.userName
            password credential.password
        }
        json
    }




    /**
     * Artifactory
     */


    def artifactoryConfigJson = {confName, artifactUrl, username, pass, logLev ->
        json.names {
            configName confName
            artifactoryUrl artifactUrl
            userName username
            password pass
            logLevel logLev
        }
        json
    }

    def artifactoryPushJson = {configName,
                               artifactName,
                               artifactDir,
                               artClassifier,
                               repo,
                               repoLayout,
                               repoPath,
                               organization,
                               organizationPath,
                               artifactExtension,
                               repositoryType, artifactType, artifactVersion ->
        json.names {
            artifact artifactName
            artifactPath artifactDir
            classifier artClassifier
            config configName
            extension artifactExtension
            org organization
            orgPath organizationPath
            repository repo
            repositoryLayout repoLayout
            repositoryPath repoPath
            repoType repositoryType
            type artifactType
            version artifactVersion
        }
        json
    }





}
