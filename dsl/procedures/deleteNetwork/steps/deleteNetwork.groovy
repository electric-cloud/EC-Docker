$[/myProject/scripts/preamble]

String clusterName = '$[clusterName]'
String envProjectName = '$[envProjectName]'
String serviceProjectName = '$[serviceProjectName]'
// default cluster project name if not explicitly set
if (!envProjectName) {
    envProjectName = serviceProjectName
}
String environmentName = '$[environmentName]'
def pluginProjectName = '$[/myProject/projectName]'

def networkName = '$[networkName]'

EFClient efClient = new EFClient()

def clusterParameters = efClient.getProvisionClusterParameters(clusterName, envProjectName, environmentName)
def pluginConfig = efClient.getConfigValues('ec_plugin_cfgs', clusterParameters.config, pluginProjectName)
DockerClient dockerClient = new DockerClient(pluginConfig)

dockerClient.deleteNetwork(networkName)