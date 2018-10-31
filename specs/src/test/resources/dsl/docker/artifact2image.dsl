package dsl.docker

def names = args.names,
        credentialName = names.credentialName,
        configName = names.configName,
        imageName = names.imageName,
        artifactName = names.artifactName,
        artifactLocation = names.artifactLocation,
        artifactoryArtifactName = names.artifactoryArtifactName,
        artifactoryConfig = names.artifactoryConfig,
        artifactoryArtifactExtension = names.artifactoryArtifactExtension,
        artifactoryOrgPath = names.artifactoryOrgPath,
        artifactoryRepoKey = names.artifactoryRepoKey,
        artifactoryRepoType = names.artifactoryRepoType,
        artifactoryArtifactVersion  = names.artifactoryArtifactVersion,
        exposePorts = names.exposePorts,
        env = names.env,
        cmd = names.cmd,
        baseImage = names.baseImage,
        registryUrl = names.registryUrl,
        artifactVersion = names.artifactVersion,
        removeAfterPush = names.removeAfterPush,
        userName = names.userName,
        password = names.password


def pluginProjectName = getPlugin(pluginName: "EC-Docker").projectName

runProcedure(
        projectName: pluginProjectName,
        procedureName: 'Artifact2Image',
        actualParameter: [
                ecp_docker_credential: credentialName,
                config: configName,
                ecp_docker_imageName: imageName,
                ecp_docker_artifactName: artifactName,
                ecp_docker_artifactLocation: artifactLocation, // Specify the URI of artifact location on file system=
                // Artifactory options
                ecp_docker_artifactoryArtifactName: artifactoryArtifactName,
                ecp_docker_artifactoryConfigName: artifactoryConfig,
                ecp_docker_artifactoryExtension: artifactoryArtifactExtension,
                ecp_docker_artifactoryOrgPath: artifactoryOrgPath,
                ecp_docker_artifactoryRepoKey: artifactoryRepoKey,
                ecp_docker_artifactoryRepoType: artifactoryRepoType,
                ecp_docker_artifactoryVersion: artifactoryArtifactVersion,
                // Image options
                ecp_docker_ports: exposePorts,
                ecp_docker_env: env,
                ecp_docker_command: cmd,
                ecp_docker_baseImage: baseImage,
                ecp_docker_registryUrl: registryUrl,
                ecp_docker_versionRange: artifactVersion,
                ecp_docker_removeAfterPush: removeAfterPush
        ],
        credential: [
                credentialName: credentialName,
                userName: userName,
                password: password
        ]
)