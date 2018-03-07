

import com.electriccloud.client.groovy.ElectricFlow
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import de.gesellix.docker.client.builder.BuildContextBuilder
import groovy.json.JsonBuilder


class LiftAndShift extends BaseClient {
    @Lazy
    ElectricFlow ef = { new ElectricFlow() }()

    File artifactCacheDirectory
    DockerClient dockerClient
    static final String WAR = 'war'
    static final String JAR = 'jar'

    /**
     *
     * @param artifact
     * @param dockerfileDetails [
     *      ENV: env entry
     *      PORTS: EXPOSE entry
     *      BASE_IMAGE: base image for dockerfile
 *          COMMAND: command for dockerfile
     * ]
     * @return
     */
    File generateDockerfile(File artifact, Map dockerfileDetails) {
        String type = getArtifactType(artifact)
        String dockerfile = buildDockerfile(dockerfileDetails, type, artifact)
        logger INFO, "Dockefile: ${dockerfile}"
        new File(artifact.parentFile, "Dockerfile").write(dockerfile)
        logger INFO, "Saved Dockerfile under ${artifact.parentFile}/Dockerfile"
        artifact.parentFile
    }

    String buildImage(String tag, File workspace) {
        File destination = new File(workspace, "image.tar")
        BuildContextBuilder.archiveTarFilesRecursively(
            workspace,
            destination
        )

        InputStream tar = new FileInputStream(destination)
        def response = dockerClient.buildImage(tag, tar)
        String imageId = response.imageId
        String log = response.log
        logger INFO, "Image has been built successfully: ${imageId}"
        if (log) {
            logger INFO, "Build log: ${log}"
        }
        return imageId
    }


    def pushImage(String imageName, String registryURL = null, String userName = null, String pass = null) {
        String auth
        if (userName && pass) {
            def json = new JsonBuilder()
            def jsonAuth = json {
                username userName
                password pass
                if (registryURL) {
                    serveraddress registryURL
                }
            }

            logger DEBUG, "Auth: ${json.toPrettyString()}"
            auth = json.toString().bytes.encodeBase64().toString()
        }
        def response = dockerClient.pushImage(imageName, auth, registryURL)
        def content = response.content
        content.each {
            logger INFO, "${it}"
        }
    }


    String buildDockerfile(Map details, String type, File artifact) {
        String name
        switch(type) {
            case JAR:
                name = 'springboot'
                break
            case WAR:
                name = 'tomcat'
                break
        }
        String templatePath = '/plugins/EC-Docker/project/dockerfiles/defaults/' + name
        String templateText = ef.getProperty_0(propertyName: templatePath)?.property?.value
        assert templateText : "Template ${templatePath} was not found"
        logger DEBUG, "Template: ${templateText}"
        Template template = new SimpleTemplateEngine().createTemplate(templateText)
        def map = details
        map.FILENAME = artifact.name
        logger DEBUG, "Template parameters: ${map}"
        String dockerfile = template.make(map)
        return dockerfile
    }

    String getArtifactType(File artifact) {
        if (artifact.name.endsWith(".jar")) {
            return JAR
        }
        else if (artifact.name.endsWith(".war")) {
            return WAR
        }
    }

    File findArtifact() {
        File artifact
        artifactCacheDirectory.eachFile {File file ->
            if (file.name.endsWith(".war") || file.name.endsWith(".jar")) {
                artifact = file
            }
            else {
                throw new PluginException("Cannot process artifact ${file.name}")
            }
        }
        logger INFO, "Artifact to be converted to Docker image: ${artifact.name}"
        return artifact
    }

}
