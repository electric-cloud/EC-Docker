import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import de.gesellix.docker.client.builder.BuildContextBuilder
import groovy.json.JsonBuilder
import sun.reflect.generics.reflectiveObjects.NotImplementedException


class LiftAndShift extends BaseClient {

    File artifactCacheDirectory
    DockerClient dockerClient

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
    File generateDockerfile(Artifact artifact, Map dockerfileDetails, String template) {
        logger INFO, "Artifact type: ${artifact.type}"
        logger INFO, "Main file: ${artifact.entrypoint}"
        logger INFO, "Dockefile template: ${template}"
        String dockerfile = buildDockerfile(dockerfileDetails, artifact.entrypoint, template)
        logger INFO, "Dockefile: ${dockerfile}"
        File workspace = artifact.entrypoint.parentFile
        new File(workspace, "Dockerfile").write(dockerfile)
        logger INFO, "Saved Dockerfile under ${workspace}/Dockerfile"
        return workspace
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

    String buildDockerfile(Map details, File entrypoint, String templateText) {
        Template template = new SimpleTemplateEngine().createTemplate(templateText)
        def map = details
        map.FILENAME = entrypoint.name
        logger DEBUG, "Template parameters: ${map}"
        String dockerfile = template.make(map)
        return dockerfile
    }
    
    def getArtifact() {
        return Artifact.findArtifact(artifactCacheDirectory)
    }


}

class Artifact {
    String type
    File artifact
    File entrypoint

    static final String WAR = 'war'
    static final String JAR = 'jar'
    static final String ASPNET = 'asp.net'

    static def findArtifact(File cacheDirectory) {
        String type
        File artifact
        File entrypoint
        cacheDirectory.eachFile { File f ->
//                TODO handle multiple files
            if (f.name.endsWith(".war") || f.name.endsWith(".jar")) {
                if (f.name.endsWith('.war')) {
                    type = WAR
                } else {
                    type = JAR
                }
                artifact = f
                entrypoint = f
            } else if (f.name.compareToIgnoreCase("web.config") == 0) {
                def config = new XmlSlurper().parseText(f.text)
                def aspNetCore = config.getProperty("system.webServer")?.aspNetCore
                if (aspNetCore) {
                    def processPath = aspNetCore.@processPath?.toString()
                    if (processPath == 'dotnet') {
                        //<?xml version="1.0" encoding="utf-8"?>
                        //<configuration>
                        //<system.webServer>
                        //<handlers>
                        //<add name="aspNetCore" path="*" verb="*" modules="AspNetCoreModule" resourceType="Unspecified" />
                        //</handlers>
                        //<aspNetCore processPath="dotnet" arguments=".\aspnetapp.dll" stdoutLogEnabled="false" stdoutLogFile=".\logs\stdout" />
                        //</system.webServer>
                        //</configuration>
                        def arguments = aspNetCore.@arguments.toString()
                        arguments = arguments.replaceAll(/\.[\/\\]/, '')
                        entrypoint = new File(cacheDirectory, arguments)
                        println entrypoint.absolutePath
                        if (entrypoint.exists()) {
                            type = ASPNET
                            artifact = cacheDirectory
                        }
                    }
                }
            } else if (f.name.endsWith(".dll")) {
                type = type ?: ASPNET
                entrypoint = entrypoint ?: f
                artifact = artifact ?: cacheDirectory
            } else if (f.name.endsWith(".csproj")) {
                throw new NotImplementedException();
            }
        }
        if (type && artifact && entrypoint) {
            return new Artifact(type: type, entrypoint: entrypoint, artifact: artifact)
        } else {
            throw new PluginException("Cannot process ${cacheDirectory.name}: no supported artifacts found")
        }
    }


    def getTemplateName() {
        if (type == WAR) {
            return 'jetty'
        } else if (type == JAR) {
            return 'springboot'
        } else if (type == ASPNET) {
            return 'aspnet'
        } else {
            throw new PluginException("Cannot find template for type ${type}")
        }
    }
}

