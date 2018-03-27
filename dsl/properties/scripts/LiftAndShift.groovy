import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import de.gesellix.docker.client.builder.BuildContextBuilder
import groovy.json.JsonBuilder
import org.apache.commons.lang.RandomStringUtils

import java.nio.file.Files
import java.nio.file.Paths


import static Logger.*

class LiftAndShift extends BaseClient {

    Artifact artifact
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
    File generateDockerfile(Map details, String templateText) {
        logger INFO, "Artifact type: ${artifact.type}"
        logger INFO, "Main file: ${artifact.entrypoint}"
        logger INFO, "Dockefile template: ${templateText}"

        if (artifact.type == Artifact.CSPROJ && !details.COMMAND) {
            logger WARNING, "No command specified in details for .csproj"
        }

        def map = details
        map.FILENAME = artifact.entrypoint.name

        String dockerfile = makeDockerfile(templateText, map)

        logger INFO, "Dockefile: ${dockerfile}"
        File workspace = artifact.entrypoint.parentFile
        new File(workspace, "Dockerfile").write(dockerfile)
        logger INFO, "Saved Dockerfile under ${workspace}/Dockerfile"
        return workspace
    }

    String makeDockerfile(String tmpl, Map params) {
        def defaults = [:]
        def lines = []
        tmpl.eachLine { line ->
            def match = (line =~ /\$\{([^${}:]+):([^}]+)\}/)
            if (match.hasGroup() && match.size() == 1 && match[0].size() >= 2) {
                def variable = match[0][1]
                def defaultValue = match[0][2]
                defaultValue = defaultValue.replaceAll(/^['"]/, '').replaceAll(/["']$/, '')
                defaults[variable] = defaultValue
                line = (line =~ /\:['"]?$defaultValue['"]?/).replaceFirst('')
            }
            lines << line
        }
        tmpl = lines.join("\n")
        Template template = new SimpleTemplateEngine().createTemplate(tmpl)
        params.keySet().each { key ->
            if (!params[key]) {
                params[key] = defaults[key]
            }
        }
        template.make(params)
    }

    String buildImage(String tag, File workspace) {
        if (!workspace.directory) {
            throw new RuntimeException("Workspace must be a directory")
        }

        File destination = new File(workspace.parentFile, "${randomString()}_image.tar")
        logger INFO, "Packing workspace: ${workspace.absolutePath} to ${destination.absolutePath}"
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
        destination.delete()
        logger INFO, "Deleted ${destination.absolutePath}"
//        workspace.deleteDir()
//        logger INFO, "Deleted workspace ${workspace.absolutePath}"
        return imageId
    }

    static String randomString() {
        String charset = (('A'..'Z') + ('0'..'9')).join()
        Integer length = 9
        String prefix = RandomStringUtils.random(length, charset.toCharArray())
        prefix
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
        if (response.status.success) {
            def content = response.content
            content.each {
                logger INFO, "${it}"
            }
        }
        else {
            throw new PluginException("Cannot push image: ${response.content}")
        }
    }


    def removeImage(String imageId) {
        def response = dockerClient.removeImage(imageId)
        if (response.status.success) {
            logger INFO, "The image ${imageId} has been removed"
        }
        else {
            logger WARNING, "Cannot remove image: ${response.content.message}"
        }
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
    static final String CSPROJ = 'csproj'


    static def fromFileSystem(File location, boolean copy = false) {
        if (!location.exists()) {
            throw new PluginException("Location ${location.absolutePath} does not exist")
        }
        if (copy) {
            String charset = (('A'..'Z') + ('0'..'9')).join()
            Integer length = 9
            String random = RandomStringUtils.random(length, charset.toCharArray())

            def target = new File("dockerfile-${location.name.replaceAll("\\W+", '-')}-${random}").canonicalFile
            if (location.directory) {
                copyDirectory(location, target)
            }
            else {
                File dest = new File(target, location.name)
                target.mkdir()
                Files.copy(Paths.get(location.absolutePath), Paths.get(dest.absolutePath))
            }
            return findArtifact(target)
        }

        if (location.directory) {
            return findArtifact(location)
        }
        else {
            String type = guessArtifactType(location)
            return new Artifact(entrypoint: location, type: type, artifact: location)
        }
    }


    static def copyDirectory(File from, File to) {
        if (!to.exists()) {
            to.mkdir()
        }
        from.eachFileRecurse { File file ->
            File destination = new File(to, from.toURI().relativize(file.toURI()).toString())
            Files.copy(Paths.get(file.absolutePath), Paths.get(destination.absolutePath))
        }
    }

    static def fromArtifactory() {

    }

    static def guessArtifactType(File artifact) {
        if (artifact.name.endsWith(".war")) {
            return WAR
        }
        else if (artifact.name.endsWith(".jar")) {
            return JAR
        }
        else {
            throw new PluginException("Cannot process artifact: ${artifact.name}")
        }
    }

    static def findArtifact(File cacheDirectory) {
        String type
        File artifact
        File entrypoint
        if (!cacheDirectory.directory) {
            throw new RuntimeException("findArtifact works with directory only")
        }
        cacheDirectory.eachFile { File f ->
//                TODO handle multiple files
            if (f.name.endsWith(".war") || f.name.endsWith(".jar")) {
                type = guessArtifactType(f)
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
                type = CSPROJ
                artifact = cacheDirectory
                entrypoint = f
            }
        }
        if (type && artifact) {
            return new Artifact(type: type, entrypoint: entrypoint, artifact: artifact)
        } else {
            throw new PluginException("Cannot process ${cacheDirectory.absolutePath}: no supported artifacts found")
        }
    }


    def getTemplateName() {
        if (type == WAR) {
            return 'jetty'
        } else if (type == JAR) {
            return 'springboot'
        } else if (type == ASPNET) {
            return 'aspnet'
        } else if (type == CSPROJ) {
            return 'csproj'
        }
        else {
            throw new PluginException("Cannot find template for type ${type}")
        }
    }
}

