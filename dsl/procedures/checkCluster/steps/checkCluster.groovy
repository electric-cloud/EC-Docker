$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

String endpoint = pluginConfig.endpoint
boolean swarmMode = pluginConfig.swarmMode.toBoolean()
DockerClient dockerClient = new DockerClient()
def resp = dockerClient.checkHealth(endpoint, swarmMode)
if (resp == "reachable"){ 
	efClient.logger INFO, "The docker endpoint is reachable at ${endpoint}"
} else {
	efClient.handleProcedureError("The docker endpoint at ${endpoint} was not reachable. Health check at $endpoint failed with $resp")
}

