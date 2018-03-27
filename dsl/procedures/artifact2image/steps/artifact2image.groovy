$[/myProject/scripts/preamble]
$[/myProject/scripts/LiftAndShift]

import com.electriccloud.client.groovy.ElectricFlow
import groovy.json.JsonSlurper
import groovy.json.JsonException


String configName = '$[config]'
String artifactName = '$[ecp_docker_artifactName]'
String imageName = '$[ecp_docker_imageName]'
String registryUrl = '$[ecp_docker_registryUrl]'
String credentialName = '$[ecp_docker_credential]'
String baseImage = '$[ecp_docker_baseImage]'
String ports = '''$[ecp_docker_ports]'''.trim()
String command = '''$[ecp_docker_command]'''.trim()
String env = '''$[ecp_docker_env]'''.trim()
String artifactFileLocation = '''$[ecp_docker_artifactLocation]'''.trim()
boolean removeAfterPush = '$[ecp_docker_removeAfterPush]'.trim() == "true"

ElectricFlow ef = new ElectricFlow()
EFClient efClient = new EFClient()


if (artifactFileLocation && artifactName) {
    efClient.handleProcedureError("Either artifact file location or artifact name should be provided, but not both")
}

String userName
String password

if (credentialName) {
    def credential = ef.getFullCredential(
        jobStepId: System.getenv('COMMANDER_JOBSTEPID'),
        credentialName: credentialName
    )

    userName = credential?.credential?.userName
    password = credential?.credential?.password
    if (!userName) {
        efClient.handleProcedureError("Username was not found in the credential ${credentialName}")
    }
}

String projectName = '$[/myProject/projectName]'
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, projectName)
DockerClient dockerClient = new DockerClient(pluginConfig)

Artifact artifactLocator

try {

    if (artifactName) {
        String artifactLocation = ef.getProperty(
            propertyName: "/myJob/${artifactName}/location"
        )?.property?.value
        // From ef repository
        artifactLocator = Artifact.fromFileSystem(new File(artifactLocation), false)
    } else if (artifactFileLocation) {
        artifactLocator = Artifact.fromFileSystem(new File(artifactFileLocation), true)
    }
    else {
        throw new PluginException("Either artifact location or artifact name should be specified")
    }
} catch (PluginException e) {
    efClient.handleProcedureError(e.getMessage())
}

def liftAndShift = new LiftAndShift(
    dockerClient: dockerClient,
    artifact: artifactLocator,
)

String resultPropertySheet = "/myParent/parent"

try {
    def details = [
        COMMAND: command,
        ENV: env,
        BASE_IMAGE: baseImage,
        PORTS: ports
    ]
    String templateName = artifactLocator.getTemplateName()
    String fullTemplatePath = "/projects/$[/myProject/projectName]/dockerfiles/defaults/${templateName}"
    String template = ef.getProperty(propertyName: fullTemplatePath)?.property?.value
    assert template : "Template ${templateName} is not found ($fullTemplatePath)"

    File dockerfileWorkspace = liftAndShift.generateDockerfile(details, template)
    String imageId = liftAndShift.buildImage(imageName, dockerfileWorkspace)
    liftAndShift.pushImage(imageName, registryUrl, userName, password)
    ef.setProperty(propertyName: '/myJobStep/summary', value: "Image ID: ${imageId}")
    ef.setProperty(propertyName: "${resultPropertySheet}/${imageName}/imageId", value: imageId)
    if (removeAfterPush) {
        liftAndShift.removeImage(imageId)
    }

} catch (PluginException e) {
    efClient.handleProcedureError(e.getMessage())
}



def parseEnvVariables(String env) {
    def envVars = []
    try {
        def o = new JsonSlurper().parseText(env)
        if (o instanceof List) {
            envVars = o.collect {
                [name: it.name, value: it.value]
            }
        }
        else if (o instanceof Map) {
            envVars = o.collect {k, v ->
                [name: k, value: v]
            }
        }
        else {
            throw new PluginException("Environment variables should be either map or list!")
        }
    } catch (JsonException e) {
        env.split(/\n+/).each { line ->
            if (line) {
                def keyValue = line.split(/\s*=\s*/)
                if (keyValue.size() == 2) {
                    envVars << [name: keyValue[0], value: keyValue[1]]
                }
                else {
                    throw new PluginException("Wrong line in environment variables: ${line}")
                }
            }
        }
    }
    envVars
}

