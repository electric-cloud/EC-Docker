@Grab("de.gesellix:docker-client:2017-06-25T15-38-14")
@Grab(group='ch.qos.logback', module='logback-classic', version='1.0.13')

import de.gesellix.docker.client.image.ManageImage
import de.gesellix.docker.client.DockerClientImpl

$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

def tempDir = System.getProperty("java.io.tmpdir")
System.setProperty("docker.cert.path","${tempDir}/certs")
System.setProperty("docker.tls.verify", "1")
def dockerClient = new DockerClientImpl(pluginConfig.endpoint)
def info = dockerClient.info().content

println info

/*
String endpoint = pluginConfig.endpoint
boolean swarmMode = pluginConfig.swarmMode.toBoolean()
DockerClient dockerClient = new DockerClient()
def resp = dockerClient.checkHealth(endpoint, swarmMode)
if (resp == "reachable"){ 
	efClient.logger INFO, "The docker endpoint is reachable at ${endpoint}"
} else {
	efClient.handleProcedureError("The docker endpoint at ${endpoint} was not reachable. Health check at $endpoint failed with $resp")
}
*/

