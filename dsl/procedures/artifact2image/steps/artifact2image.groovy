$[/myProject/scripts/preamble]
$[/myProject/scripts/LiftAndShift]

import com.electriccloud.client.groovy.ElectricFlow
import groovy.json.JsonSlurper
import groovy.json.JsonException


String configName = '$[config]'
String artifactName = '$[ec_docker_artifactName]'
String imageName = '$[ec_docker_imageName]'
String registryUrl = '$[ec_docker_registryUrl]'
String credentialName = '$[ec_docker_credential]'
String baseImage = '$[ec_docker_baseImage]'
String ports = '''$[ec_docker_ports]'''.trim()
String command = '''$[ec_docker_command]'''.trim()
String env = '''$[ec_docker_env]'''.trim()

ElectricFlow ef = new ElectricFlow()
EFClient efClient = new EFClient()

String artifactLocation = ef.getProperty(
    propertyName: "/myJob/${artifactName}/location"
)?.property?.value

if (!artifactLocation) {
    efClient.handleProcedureError("Artifact location property was not found")
}


File artifactFolder = new File(artifactLocation)
if (!artifactFolder.exists()) {
    efClient.handleProcedureError("Artifact location does not exist: ${artifactLocation}")
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

def liftAndShift = new LiftAndShift(
    dockerClient: dockerClient,
    artifactCacheDirectory: artifactFolder
)

String resultPropertySheet = "/myParent/parent"

try {
    File artifact = liftAndShift.findArtifact()
    def details = [
        COMMAND: command,
        ENV: env,
        BASE_IMAGE: baseImage,
        PORTS: ports
    ]
    File dockerfileWorkspace = liftAndShift.generateDockerfile(artifact, details)
    String imageId = liftAndShift.buildImage(imageName, dockerfileWorkspace)
    liftAndShift.pushImage(imageName, registryUrl, userName, password)
    ef.setProperty(propertyName: '/myJobStep/summary', value: "Image ID: ${imageId}")
    ef.setProperty(propertyName: "${resultPropertySheet}/${imageName}/imageId", value: imageId)

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
