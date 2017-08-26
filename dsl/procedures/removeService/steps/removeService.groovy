$[/myProject/scripts/preamble]

def pluginProjectName = '$[/myProject/projectName]'
// Input parameters
def configName = '$[config]'
def serviceName = '$[serviceName]'

EFClient efClient = new EFClient()
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', configName, pluginProjectName)

DockerClient dockerClient = new DockerClient(pluginConfig)
dockerClient.undeployDockerService(serviceName)
