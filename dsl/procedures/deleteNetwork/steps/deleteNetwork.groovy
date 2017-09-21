$[/myProject/scripts/preamble]


def pluginProjectName = '$[/myProject/projectName]'
def pluginConfig = '$[pluginConfig]'
def networkName = '$[networkName]'

EFClient efClient = new EFClient()
def pluginConfigValues = efClient.getConfigValues('ec_plugin_cfgs', pluginConfig, pluginProjectName)
DockerClient dockerClient = new DockerClient(pluginConfigValues)

dockerClient.deleteNetwork(networkName)