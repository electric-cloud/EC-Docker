
import com.electriccloud.client.groovy.ElectricFlow
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import de.gesellix.docker.client.builder.BuildContextBuilder
import groovy.json.JsonBuilder


class LiftAndShift {
    @Lazy
    ElectricFlow ef = { new ElectricFlow() }()

    File artifactCacheDirectory
    DockerClient dockerClient
    static final String WAR = 'war'
    static final String JAR = 'jar'

    File generateDockerfile(File artifact, Map dockerfileDetails) {
        String type = getArtifactType(artifact)
        String dockerfile = buildDockerfile(dockerfileDetails, type, artifact)
        logger INFO, "Dockefile: ${dockerfile}"
        new File(artifact.parentFile, "Dockerfile").write(dockerfile)
        logger INFO, "Saved Dockerfile"
        artifact.parentFile
    }

    String buildImage(String tag, File workspace) {
        File destination = new File(workspace, "image.tar")
        BuildContextBuilder.archiveTarFilesRecursively(
            workspace,
            destination
        )

        InputStream tar = new FileInputStream(destination)
        String imageId = dockerClient.buildImage(tag, tar)
        imageId
    }


    def pushImage(String imageId, String registryURL = null, String userName = null, String password = null) {
        String auth
        if (userName && password) {
            def json = new JsonBuilder()
            def jsonAuth = json {
                username userName
                password password
                if (registryURL) {
                    serveraddress registryURL
                }
            }
            logger DEBUG, "Auth: ${jsonAuth.toPrettyString()}"
            auth = jsonAuth.toString().bytes.encodeBase64().toString()
        }
        def response = dockerClient.pushImage(imageId, auth, registryURL)
        println response
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
        String templateText = ef.getProperty_0(propertyName: '/plugins/EC-Docker/project/dockerfiles/defaults/' + name)
        println templateText
        Template template = new SimpleTemplateEngine().createTemplate(templateText)
        def map = [:]
        map.FILENAME = artifact.name
        map.COMMAND = details.dockerCommand
        map.ENV = details.env ?: []
        map.PORTS = details.ports ?: []
        map.BASE_IMAGE = details.image
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

        return artifact
    }

}
