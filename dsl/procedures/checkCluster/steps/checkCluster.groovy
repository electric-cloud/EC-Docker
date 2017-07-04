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

if (pluginConfig.credential_key.password){
	// If docker client private key is provided in plugin config then enable TLS mode
	System.setProperty("docker.tls.verify", "1")
}

def dockerClient = new DockerClientImpl(pluginConfig.endpoint)
def info = dockerClient.info().content
println info

if (pluginConfig.swarmMode=="true"){

    try{
		def role = dockerClient.inspectNode(pluginConfig.swarmManagerHostname).content.Spec.Role
		if (role != "manager"){
			// Given node is  worker node in swarm cluster
			logger ERROR, "${pluginConfig.endpoint} is not Swarm Manager. Exiting.."
			exit 1
		}

		def availability = dockerClient.inspectNode(pluginConfig.swarmManagerHostname).content.Spec.Availability
		if (availability!="active"){
			// Given node is not active manager
			logger ERROR, "${pluginConfig.endpoint} is not active Swarm Manager. Exiting.."
			exit 1
		}
	}catch(Exception e){
		// Given node is not a swarm manager
		logger ERROR, "${e}"
		logger ERROR, "${pluginConfig.endpoint} is not Swarm Manager. Exiting.."
		exit 1
	}

}


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

